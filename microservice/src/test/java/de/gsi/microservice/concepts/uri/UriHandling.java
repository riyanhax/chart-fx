package de.gsi.microservice.concepts.uri;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UriHandling {
    public static void main(String[] argv) {
        final String uriExample1 = "https://TestUser@www.opencmw.org:2021/device/property/field?ctx=FAIR.SELECTOR.C=1&otherFilter#testFragment";
        final String uriExample2 = "(application/string)://TestUser@www.opencmw.org:2021/device/property/field?ctx=FAIR.SELECTOR.C=1&otherFilter#testFragment";
        final String uriExampleError = "https://www.opencmw.org:2021/device/property@field";

        final URI uri1 = URI.create(uriExample1);
        System.out.println("uri#1: " + uri1.toString());
        System.out.println("scheme: " + uri1.getScheme());
        System.out.println("user: " + uri1.getUserInfo());
        System.out.println("authority: " + uri1.getAuthority());
        System.out.println("host: " + uri1.getHost());
        System.out.println("port: " + uri1.getPort());
        System.out.println("path: " + uri1.getPath());
        System.out.println("param: " + uri1.getQuery());
        System.out.println("fragment: " + uri1.getFragment());
        System.out.println("\n");

        try {
            URL url = uri1.toURL();
            System.out.println("urL#1: " + url.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        final URI uri2 = URI.create(uriExample2);
        System.out.println("uri#2: " + uri2.toString());
        System.out.println("scheme: " + uri2.getScheme());
        System.out.println("user: " + uri2.getUserInfo());
        System.out.println("fragment: " + uri2.getFragment());
        System.out.println("\n");

        final URI uri3;
        try {
            uri3 = new URI(uriExampleError);
            System.out.println("uri#3: " + uri3.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
