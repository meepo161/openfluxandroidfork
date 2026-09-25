package mobile

import (
	"encoding/json"
	"fmt"
	"sort"

	"openflux/transport"
	"openflux/transport/manager"
)

// sessionSpec is one transport of a Session profile, as the app sends it.
// Names must match the exit's (the CLI names --transports entries after
// their type) because cookie exchange is addressed by name.
type sessionSpec struct {
	Name     string                 `json:"name"`
	Type     string                 `json:"type"`
	URL      string                 `json:"url"`
	Priority int                    `json:"priority"`
	Params   map[string]interface{} `json:"params"`
}

// buildSession mirrors the CLI client's --negotiate / --transports path, so
// the phone talks to an exit started with the same transports and --url.
// specsJSON is a JSON array of sessionSpec. exit builds the exit node's
// side (the phone as an l4 exit).
func buildSession(specsJSON, secret string, exit bool) (transport.Transport, error) {
	var specs []sessionSpec
	if err := json.Unmarshal([]byte(specsJSON), &specs); err != nil {
		return nil, fmt.Errorf("список транспортов: %w", err)
	}
	if len(specs) == 0 {
		return nil, fmt.Errorf("список транспортов пуст")
	}
	if len(secret) < 16 {
		return nil, fmt.Errorf("для режима Session нужен ключ шифрования не короче 16 символов")
	}

	// Like the CLI: an l4 exit terminates flows in gVisor and has no raw
	// ICMP errors to relay; a client does.
	caps := transport.CapabilityIPv4 | transport.CapabilityTCP | transport.CapabilityUDP
	if !exit {
		caps |= transport.CapabilityICMPErrors
	}
	sess, err := transport.NewSession(transport.PeerParameters{
		Capabilities:  caps,
		MaxPacketSize: transport.MaxNegotiatedPacket,
	}, exit)
	if err != nil {
		return nil, err
	}
	context := sessionContext(specs)
	m := manager.New(sess, nil, secret, context)
	config := transport.DefaultConfig()
	keys := make(map[string]string)
	types := make(map[string]string)
	for _, spec := range specs {
		types[spec.Name] = spec.Type
		raw, err := newRawTransport(spec.Type, spec.URL, spec.Params, config, exit)
		if err != nil {
			return nil, fmt.Errorf("%s: %w", spec.Name, err)
		}
		if err := sess.AddTransport(spec.Name, raw, secret, context, spec.Priority); err != nil {
			return nil, err
		}
		provider, _ := raw.(manager.CookieProvider)
		if err := m.Add(spec.Name, spec.Type, raw, spec.Priority, provider); err != nil {
			return nil, err
		}
		if provider != nil {
			keys[spec.Name] = spec.Type + " " + spec.URL
		}
		appendLog(fmt.Sprintf("[ANDROID] Session: транспорт %s (%s), приоритет %d", spec.Name, spec.Type, spec.Priority))
	}
	sess.SetControlHandler(m.DispatchControl)
	setSessionRoute(sess, types)
	if exit {
		// The exit relays its own checks to the client itself (AuthRequired);
		// the phone's UI can still pass them locally.
		attachSessionCaptcha(m, keys, nil)
		appendLog("[ANDROID] Session: шифрование AES-256-GCM, ожидание клиента")
		return m, nil
	}

	// The side stack for exit checks shares the tunnel; a PortDemux hands
	// it the replies to its ports and everything else to the regular path.
	demux := transport.NewPortDemux(m, authProxyPortLo, authProxyPortHi)
	proxy := &authProxy{demux: demux}
	setAuthProxy(proxy)
	attachSessionCaptcha(m, keys, proxy)
	appendLog("[ANDROID] Session: шифрование AES-256-GCM, согласование с нодой")
	return demux, nil
}

// sessionContext is the encryption context. The exit derives it from its
// --url, so it is the document URL of the highest-priority transport that
// has one; "http://#" matches the CLI's --url default when none has.
func sessionContext(specs []sessionSpec) string {
	sorted := append([]sessionSpec(nil), specs...)
	sort.SliceStable(sorted, func(i, j int) bool { return sorted[i].Priority > sorted[j].Priority })
	for _, s := range sorted {
		if s.URL != "" {
			return s.URL
		}
	}
	return "http://#"
}
