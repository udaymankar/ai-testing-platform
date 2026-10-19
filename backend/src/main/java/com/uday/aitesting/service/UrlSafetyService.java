package com.uday.aitesting.service;

import org.springframework.stereotype.Service;
import java.net.*;
import java.util.Locale;
import java.util.Set;

@Service
public class UrlSafetyService {
    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "localhost", "localhost.localdomain", "metadata.google.internal"
    );

    public URI validateAndNormalize(String raw) {
        try {
            URI uri = new URI(raw.trim()).normalize();
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
                throw new IllegalArgumentException("Only http and https URLs are allowed.");
            }
            if (uri.getUserInfo() != null) throw new IllegalArgumentException("URLs containing credentials are not allowed.");
            String host = uri.getHost();
            if (host == null || host.isBlank()) throw new IllegalArgumentException("A valid hostname is required.");
            host = host.toLowerCase(Locale.ROOT);
            if (BLOCKED_HOSTS.contains(host) || host.endsWith(".localhost") || host.endsWith(".local")) {
                throw new IllegalArgumentException("Local hostnames are not allowed.");
            }
            if (isIpLiteral(host)) {
                InetAddress address = InetAddress.getByName(host);
                if (isBlockedAddress(address)) throw new IllegalArgumentException("Private or reserved IP addresses are not allowed.");
            } else {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                if (addresses.length == 0) throw new IllegalArgumentException("Hostname did not resolve.");
                for (InetAddress address : addresses) {
                    if (isBlockedAddress(address)) throw new IllegalArgumentException("Hostname resolves to a private or reserved address.");
                }
            }
            return uri;
        } catch (URISyntaxException | UnknownHostException ex) {
            throw new IllegalArgumentException("Invalid or unresolvable URL.");
        }
    }

    private boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private boolean isBlockedAddress(InetAddress a) {
        return a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
            || a.isSiteLocalAddress() || a.isMulticastAddress();
    }
}
