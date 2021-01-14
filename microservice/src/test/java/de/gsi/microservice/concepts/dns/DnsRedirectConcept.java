package de.gsi.microservice.concepts.dns;

import javax.naming.NamingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import io.javalin.Javalin;
import org.jetbrains.annotations.NotNull;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Tokenizer;
import org.xbill.DNS.Type;
import org.xbill.DNS.Update;
import org.xbill.DNS.utils.base64;

public class DnsRedirectConcept {
    public static void main(String[] argv) throws NamingException, URISyntaxException, TextParseException {
        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx -> {
            StringBuilder builder = new StringBuilder();
            builder.append("HTML request:!\n");
            for (String header : Collections.list(ctx.req.getHeaderNames())) {
                builder.append("Header - ").append(header).append(": ").append(ctx.req.getHeader(header)).append("\n");
            }
            builder.append("parameter map: ").append(ctx.req.getParameterMap()).append("\n");
            builder.append("getRequestURI: ").append(ctx.req.getRequestURI()).append("\n");
            builder.append("getRequestURL: ").append(ctx.req.getRequestURL()).append("\n");
            builder.append("getContextPath: ").append(ctx.req.getContextPath()).append("\n");
            builder.append("getPathInfo: ").append(ctx.req.getPathInfo()).append("\n");
            builder.append("getMethod: ").append(ctx.req.getMethod()).append("\n");
            builder.append("getAuthType: ").append(ctx.req.getAuthType()).append("\n");
            builder.append("getRemoteUser: ").append(ctx.req.getRemoteUser()).append("\n");
            builder.append("getAuthType: ").append(ctx.req.getAuthType()).append("\n");
            builder.append("getQueryString: ").append(ctx.req.getQueryString()).append("\n");

            if (ctx.req.getHeader("Host").contains("192.168.1.5")) {
                ctx.res.sendRedirect("http://stardestroyer:7000");
            } else {
                ctx.result(builder.toString());
            }
        });

        Javalin app2 = Javalin.create().start(8080);
        app2.get("/", ctx -> {
            if (ctx.req.getHeader("Host").contains("192.168.1.5")) {
                ctx.res.sendRedirect("http://stardestroyer:7000");
            } else {
                ctx.result("unknown request");
            }
        });

        Record[] records = new Lookup("gmail.com", Type.MX).run();
        for (int i = 0; records != null && i < records.length; i++) {
            MXRecord mx = (MXRecord) records[i];
            System.out.println("Host " + mx.getTarget() + " has preference " + mx.getPriority());
        }

        testSRVRecords("stardestroyer.steinhagen.ch");
        setSRVRecords("stardestroyer.steinhagen.ch");

        //        {
        //            Hashtable env = new Hashtable();
        //            // com.sun.jndi.dns.DnsContextFactory
        //            //env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        //            env.put("java.naming.factory.initial", "com.example.jndi.dns.DnsContextFactory");
        //            env.put("java.naming.provider.url", "dns://server1.example.com/example.com");
        //
        //            DirContext ictx = new InitialDirContext(env);
        //            Attributes attrs1 = ictx.getAttributes("host1", new String[] { "A" });
        //            Attributes attrs2 = ictx.getAttributes("192.168.1.1", new String[] { "A" });
        //            System.err.println("a1 = " + attrs1);
        //            System.err.println("a2 = " + attrs2);
        //        }
        //
        //        // from: https://nitschinger.at/Bootstrapping-from-DNS-SRV-records-in-Java/
        //        //String service = "_cbnodes._tcp.example.com";
        //        String service = "_seeds._tcp.couchbase.com";
        //        //String service = " _sip._tcp.steinhagen.ch";
        //
        //        Hashtable<String, String> env = new Hashtable<String, String>();
        //        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        //        env.put("java.naming.provider.url", "dns:");
        //        DirContext dnsCtx = new InitialDirContext(env);
        //        Attributes attrs = dnsCtx.getAttributes(service, new String[] { "SRV" });
        //
        //        NamingEnumeration<?> servers = attrs.get("srv").getAll();
        //        Set<DnsRecord> sortedRecords = new TreeSet<DnsRecord>();
        //        while (servers.hasMore()) {
        //            DnsRecord record = DnsRecord.fromString((String) servers.next());
        //            sortedRecords.add(record);
        //        }
        //
        //        final List<URI> uris = new ArrayList<URI>();
        //        for (DnsRecord record : sortedRecords) {
        //            uris.add(new URI("http://" + record.getHost() + ":" + record.getPort() + "/pools"));
        //        }
        //
        //        app2.get("/uri", ctx2 -> ctx2.html(uris.toString()));
        //
        //        //        String service = "_seeds._tcp.couchbase.com";
        //        //
        //        //        BasicAttributes basicAttributes = new BasicAttributes(true);
        //        //        BasicAttribute basicAttribute = new BasicAttribute("SRV");
        //        //        basicAttribute.add("20 0 8091 node2.couchbase.com.");
        //        //        basicAttribute.add("10 0 8091 node1.couchbase.com.");
        //        //        basicAttribute.add("30 0 8091 node3.couchbase.com.");
        //        //        basicAttribute.add("40 0 8091 node4.couchbase.com.");
        //        //        basicAttributes.put(basicAttribute);
        //
        //        //        DirContext mockedContext = mock(DirContext.class);
        //        //        when(mockedContext.getAttributes(service, new String[] { "SRV" }))
        //        //                .thenReturn(basicAttributes);
    }

    public static void testSRVRecords(final String domain) {
        final String query = "_sip._tcp." + domain;

        try {
            Record[] records = new Lookup(query, Type.SRV).run();
            if (records == null) {
                System.out.println("no " + query + " SRV records found");
                return;
            }
            for (Record record : records) {
                SRVRecord srv = (SRVRecord) record;

                String hostname = srv.getTarget().toString().replaceFirst("\\.$", "");
                int port = srv.getPort();

                System.out.println(hostname + ":" + port);
            }
        } catch (TextParseException e) {
            e.printStackTrace();
        }
    }

    public static void setSRVRecords(final String server) {
        Tokenizer t = new Tokenizer("0 5 5060 " + server + ".");
        SRVRecord record = null;
        try {
            record = new SRVRecord(new Name("_sip._tcp.steinhagen.ch."), DClass.IN, 5, 10, 60, 5060, new Name("stardestroyer.steinhagen.ch."));

            Name zone = Name.fromString("dyn.test.example.");
            //Name zone = Name.fromString("steinhagen.ch.");
            Name host = Name.fromString("host", zone);
            Update update = new Update(zone);
            update.replace(host, Type.A, 3600, "192.168.1.5");
            update.add(record);

            Resolver res = new SimpleResolver("192.168.1.5");
            res.setTSIGKey(new TSIG(TSIG.HMAC_SHA256, new Name("MYKEY."), base64.fromString("z8G5C6cOUCJs9RqIMdFftMLXLg/BZZHFR54zwUN1oSrjEErpg5pXGyEeeINCovPrQSWNNgVo10XxQTPUU1/riBL0RsBsOCulXJgM8Sw3sqrok7lra7f0RXugwV7RcNPDXi4jbsBi6fd7/HL6H6shgENSNv9kACx8Bt4R/zgXHhgwjWQsJxG85qQR/4asZADx47pxwCvCD9mMCQPdZfh2mDSRTfNlJ9ot/oMESbVUIz7AOZWrsM2/pxAzS2KLUlhu/9lL2dElEikY4oaoVy9eBhmz8AHDvhhJMlYW+EWZIAlY3ousln/aGsL/rTp9bgSZNadcsEq6DauK01MzKXjuEFcNDn4HI7lCGTALCBFqjn9OA1c6rX+Rk0HSevxhwRTqlxJBVYgWdopSbaOv3G++2PqiNYa+HE3bxb2N/hGFwSQqvEhfolN+4ZnNL0iNYTgnqYuq82lt2Gmr2ux0L1hH7PYv7ZSDXzaJXNetbJQcC0XkFhsW2oGGNGoFSu0jVnwYr9AA+wHy8EBWmL2Fw3s4bXBPDlbGCgIVQ0hX9Y98Pl2ugPFFdjx8/w+RFB2jEYXwEVfF8XPhMHspNcH3V8rzonKUIOTwM/zRWM0w/Uh6CWiZBmsUYulE1P2R1QGbTd54Z2/iFw+STOkgW/7IjyeN2I3hHR5DBpuDfoBCW35m+M0=")));
            res.setTCP(true);

            Message response = res.send(update);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class DnsRecord implements Comparable<DnsRecord> {
        private final int priority;
        private final int weight;
        private final int port;
        private final String host;

        public DnsRecord(int priority, int weight, int port, String host) {
            this.priority = priority;
            this.weight = weight;
            this.port = port;
            this.host = host.replaceAll("\\\\.$", "");
        }

        public int getPriority() {
            return priority;
        }

        public int getWeight() {
            return weight;
        }

        public int getPort() {
            return port;
        }

        public String getHost() {
            return host;
        }

        public static DnsRecord fromString(String input) {
            String[] splitted = input.split(" ");
            return new DnsRecord(
                    Integer.parseInt(splitted[0]),
                    Integer.parseInt(splitted[1]),
                    Integer.parseInt(splitted[2]),
                    splitted[3]);
        }

        @Override
        public String toString() {
            return "DnsRecord{"
                    + "priority=" + priority + ", weight=" + weight + ", port=" + port + ", host='" + host + '\\' + '}';
        }

        @Override
        public int compareTo(@NotNull final DnsRecord o) {
            if (getPriority() < o.getPriority()) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
