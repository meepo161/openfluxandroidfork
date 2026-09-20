package mobile

import (
	"net"
	"strings"

	"openflux/socks5"
)

// splitDialer wraps another socks5.Dialer (the encrypted TCPTunnel) and
// dials matched domains directly from the device instead, for the Android
// "Маршрутизация" split-routing presets/custom domains. Domains are
// suffix-matched (a bare "youtube.com" also matches "www.youtube.com").
type splitDialer struct {
	tunnel socks5.Dialer
	bypass map[string]struct{}
}

func newSplitDialer(inner socks5.Dialer, bypassDomains []string) *splitDialer {
	set := make(map[string]struct{}, len(bypassDomains))
	for _, d := range bypassDomains {
		d = strings.ToLower(strings.TrimSpace(d))
		if d != "" {
			set[d] = struct{}{}
		}
	}
	return &splitDialer{tunnel: inner, bypass: set}
}

func (d *splitDialer) DialTCP(address string) (net.Conn, error) {
	host, _, err := net.SplitHostPort(address)
	if err == nil && d.matches(host) {
		return net.Dial("tcp", address)
	}
	return d.tunnel.DialTCP(address)
}

func (d *splitDialer) matches(host string) bool {
	host = strings.ToLower(host)
	for domain := range d.bypass {
		if host == domain || strings.HasSuffix(host, "."+domain) {
			return true
		}
	}
	return false
}
