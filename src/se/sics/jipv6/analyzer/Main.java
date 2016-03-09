package se.sics.jipv6.analyzer;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

import se.sics.jipv6.pcap.PCAPPacket;
import se.sics.jipv6.pcap.PCAPReader;
import se.sics.jipv6.util.SerialRadioConnection;
import se.sics.jipv6.util.Utils;

public class Main {

    private static final boolean DEBUG = false;

    private static void usage(int status) {
        System.out.println("Usage: jipv6 [-f file-to-read] [-o file-to-write] [-a host] [-p host-port] [-z analyzer] [-t timing]");
        System.exit(status);
    }

    public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
        String infile = null;
        String outfile = null;
        String analyzerName = null;
        PacketAnalyzer analyzer = null;
        String host = null;
        int port = 9999;
        int channel = -1;
        int realtime = -1;
        int delay = -1;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (i + 1 < args.length) {
                if (a.equals("-f")) {
                    infile = args[++i];
                    continue;
                }
                if (a.equals("-o")) {
                    outfile = args[++i];
                    continue;
                }
                if (a.equals("-a")) {
                    host = args[++i];
                    continue;
                }
                if (a.equals("-p")) {
                    port = Integer.parseInt(args[++i]);
                    continue;
                }
                if (a.equals("-c")) {
                    channel = Integer.parseInt(args[++i]);
                    continue;
                }
                if (a.equals("-z")) {
                    analyzerName = args[++i];
                    continue;
                }
                if (a.equals("-t")) {
                    String timing = args[++i];
                    if (timing.endsWith("%")) {
                        realtime = Integer.parseInt(timing.substring(0,  timing.length() - 1));
                    } else if(timing.endsWith("ms")) {
                        delay = Integer.parseInt(timing.substring(0,  timing.length() - 2));
                    } else if(timing.endsWith("msec")) {
                        delay = Integer.parseInt(timing.substring(0,  timing.length() - 4));
                    } else if(timing.endsWith("s")) {
                        delay = 1000 * Integer.parseInt(timing.substring(0,  timing.length() - 1));
                    } else {
                        delay = 1000 * Integer.parseInt(timing);
                    }
                    continue;
                }
            }
            if (a.equals("-h") || a.equals("--help")) {
                usage(0);
            }
            System.err.println("Illegal argument: " + a);
            usage(1);
        }

        // Setup default values
        if (infile == null) {
            if (host == null) {
                host = "localhost";
            }
        } else if (host != null) {
            System.err.println("Error: can not both read from file and connect to serial radio.");
            usage(1);
        }

        if (analyzerName == null) {
            analyzerName = TestSniff.getJarManifestProperty("DefaultPacketAnalyzer");
        }
        if (analyzerName == null) {
            analyzerName = "se.sics.jipv6.analyzer.ExampleAnalyzer";
        }
        System.err.println("# Using analyzer " + analyzerName);
        analyzer = getAnalyzer(analyzerName);
        if (analyzer == null) {
            System.err.println("Failed to create analyzer of type '" + analyzerName + "'");
            usage(1);
        }

        TestSniff sniff = new TestSniff(analyzer);
        if (infile != null) {
            System.err.println("# Reading from pcap file " + infile);
            PCAPReader reader = new PCAPReader(infile);
            reader.setStripEthernetHeaders(true);
            if (DEBUG) System.err.println("# PCAP " + reader.getVersionMajor() + "." + reader.getVersionMinor()
            + " " + reader.getLinkLayerHeaderType());
            for (PCAPPacket packet = reader.readPacket(); packet != null; packet = reader.readPacket()) {
                byte[] packetData = packet.getPayload();
                if (DEBUG) System.out.println("PCAP(" + packet.getPayloadSize() + "/" + packet.getCapturedSize() + "): " + Utils.bytesToHexString(packet.getPayload()));
                if (packetData.length > 2) {
                    // Removing ending CRC
                    packetData = Arrays.copyOfRange(packetData, 2, packetData.length - 2);
                    sniff.packetData(packet.getPayload());

                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                    if (realtime > 0 && realtime < 100) {
                        //
                    }
                }
            }
            reader.close();
            System.err.println("# [End of PCAP file]");
            System.exit(0);
        }

        if (outfile != null) {
//            System.err.println("# Exporting to log file " + outfile);
//            sniff.setOutFile(outfile);
        }

        System.err.println("# Connecting to serial radio");
        sniff.connect(host, port);

        // Change radio channel if specified
        if (channel >= 0) {
            SerialRadioConnection radio = sniff.getSerialRadio();
            if (radio != null) {
                radio.setRadioChannel(channel);
            }
        }

        sniff.runCLI();
    }

    private static PacketAnalyzer getAnalyzer(String analyzerClassName) {
        try {
            Class<?> paClass = Class.forName(analyzerClassName);
            return (PacketAnalyzer) paClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}