package io.openflux.app;

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A saved connection configuration: which transport, which document/secret,
// a display name and an icon. Lets a user keep several exit nodes configured
// and switch between them without re-typing anything.
//
// In Session mode (the CLI's --negotiate / --transports) the main transport
// is joined by extra ones, all running at once with failover by priority.
final class Profile {
    long id;
    String name = "";
    String icon = "ic_public";
    String transportType = "yandex";
    String documentUrl = "";
    String encryptionSecret = "";
    String codec = "batched";
    String maxToken = "";
    String maxUid = "";

    boolean session;
    int priority = 50;
    final List<Transport> extraTransports = new ArrayList<>();

    // One extra carrier of a Session profile.
    static final class Transport {
        String type = "direct";
        // Document URL; host:port for direct; MAX Web token for oneme.
        String value = "";
        String uid = "";
        int priority = 100;

        Transport copy() {
            Transport t = new Transport();
            t.type = type;
            t.value = value;
            t.uid = uid;
            t.priority = priority;
            return t;
        }
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("icon", icon);
        o.put("transportType", transportType);
        o.put("documentUrl", documentUrl);
        o.put("encryptionSecret", encryptionSecret);
        o.put("codec", codec);
        o.put("maxToken", maxToken);
        o.put("maxUid", maxUid);
        o.put("session", session);
        o.put("priority", priority);
        JSONArray extras = new JSONArray();
        for (Transport t : extraTransports) {
            extras.put(new JSONObject()
                    .put("type", t.type)
                    .put("value", t.value)
                    .put("uid", t.uid)
                    .put("priority", t.priority));
        }
        o.put("extraTransports", extras);
        return o;
    }

    static Profile fromJson(JSONObject o) {
        Profile p = new Profile();
        p.id = o.optLong("id");
        p.name = o.optString("name", "");
        p.icon = o.optString("icon", "ic_public");
        p.transportType = o.optString("transportType", "yandex");
        p.documentUrl = o.optString("documentUrl", "");
        p.encryptionSecret = o.optString("encryptionSecret", "");
        p.codec = o.optString("codec", "batched");
        p.maxToken = o.optString("maxToken", "");
        p.maxUid = o.optString("maxUid", "");
        p.session = o.optBoolean("session", false);
        p.priority = o.optInt("priority", 50);
        JSONArray extras = o.optJSONArray("extraTransports");
        for (int i = 0; extras != null && i < extras.length(); i++) {
            JSONObject e = extras.optJSONObject(i);
            if (e == null) continue;
            Transport t = new Transport();
            t.type = e.optString("type", "direct");
            t.value = e.optString("value", "");
            t.uid = e.optString("uid", "");
            t.priority = e.optInt("priority", 100);
            p.extraTransports.add(t);
        }
        return p;
    }

    static String transportLabel(String type) {
        if ("direct".equals(type)) return "Direct (TCP до ноды)";
        if ("vyandex".equals(type)) return "Yandex Docs (Volga)";
        if ("boards".equals(type)) return "Yandex Board";
        if ("mailru".equals(type)) return "Mail.ru Docs";
        if ("cupsonline".equals(type)) return "Cups.online";
        if ("oneme".equals(type)) return "MAX (OneMe)";
        return "Yandex Docs";
    }

    // Puts what both connection services need to start this profile; they
    // read the same extra keys.
    void putConnectionExtras(Intent intent) {
        intent.putExtra(OpenFluxTunnelService.EXTRA_DOCUMENT_URL, documentUrl);
        intent.putExtra(OpenFluxTunnelService.EXTRA_ENCRYPTION_SECRET, encryptionSecret);
        intent.putExtra(OpenFluxTunnelService.EXTRA_TRANSPORT_TYPE, transportType);
        intent.putExtra(OpenFluxTunnelService.EXTRA_CODEC, codec);
        intent.putExtra(OpenFluxTunnelService.EXTRA_MAX_TOKEN, maxToken);
        intent.putExtra(OpenFluxTunnelService.EXTRA_MAX_UID, maxUid);
        String specs = "";
        if (session) {
            try {
                specs = sessionTransportsJson();
            } catch (JSONException ignored) {
                // Unreachable for string/int values; classic mode is the fallback.
            }
        }
        intent.putExtra(OpenFluxTunnelService.EXTRA_SESSION_TRANSPORTS, specs);
    }

    // The transport list Mobile.startSession takes: the main transport plus
    // the extra ones. Names follow the CLI, which names --transports entries
    // after their type (then type-2, type-3 for repeats), so they match the
    // exit's: cookie exchange is addressed by name.
    String sessionTransportsJson() throws JSONException {
        JSONArray out = new JSONArray();
        Map<String, Integer> seen = new HashMap<>();
        String mainValue = "oneme".equals(transportType) ? maxToken : documentUrl;
        out.put(spec(seen, transportType, mainValue, maxUid, priority));
        for (Transport t : extraTransports) out.put(spec(seen, t.type, t.value, t.uid, t.priority));
        return out.toString();
    }

    private static JSONObject spec(Map<String, Integer> seen, String type, String value, String uid,
            int priority) throws JSONException {
        int n = seen.containsKey(type) ? seen.get(type) + 1 : 1;
        seen.put(type, n);
        JSONObject params = new JSONObject();
        String url = "";
        if ("direct".equals(type)) {
            params.put("dial", value);
        } else if ("oneme".equals(type)) {
            params.put("token", value);
            params.put("uid", uid);
        } else {
            url = value;
        }
        return new JSONObject()
                .put("name", n == 1 ? type : type + "-" + n)
                .put("type", type)
                .put("url", url)
                .put("priority", priority)
                .put("params", params);
    }
}
