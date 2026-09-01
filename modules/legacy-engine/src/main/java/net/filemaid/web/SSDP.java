package net.filemaid.web;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SSDP {
    public static final String MULTICAST_ADDRESS = "239.255.255.250";
    public static final int PORT = 1900;
    public static final String SERVICE_TYPE_ALL = "ssdp:all";
    public static final String SERVICE_TYPE_DLNA_MEDIA_SERVER = "urn:schemas-upnp-org:device:MediaServer:1";
    public static final int MAX_WAIT_TIME_SECONDS = 5;

    private static DatagramPacket createRequest(String string, int n, String string2, int n2) throws IOException {
        StringBuilder stringBuilder = new StringBuilder("M-SEARCH * HTTP/1.1\r\n");
        stringBuilder.append("HOST: " + string + ":" + n + "\r\n");
        stringBuilder.append("MAN: \"ssdp:discover\"\r\n");
        stringBuilder.append("MX: " + n2 + "\r\n");
        stringBuilder.append("ST: " + string2 + "\r\n");
        stringBuilder.append("\r\n");
        byte[] byArray = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(byArray, byArray.length, InetAddress.getByName(string), n);
    }

    private static Map<String, String> parseResponse(byte[] byArray) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        String string = new String(byArray, StandardCharsets.UTF_8);
        for (String string2 : string.split("\r\n")) {
            String[] stringArray = string2.split(": ", 2);
            if (stringArray.length != 2) continue;
            hashMap.put(stringArray[0], stringArray[1]);
        }
        return hashMap;
    }

    public static void discover(String string, Consumer<Map<String, String>> consumer) throws IOException {
        SSDP.discover(MULTICAST_ADDRESS, 1900, string, 5, consumer);
    }

    public static void discover(String string, int n, Consumer<Map<String, String>> consumer) throws IOException {
        SSDP.discover(MULTICAST_ADDRESS, 1900, string, n, consumer);
    }

    public static void discover(String string, int n, String string2, int n2, Consumer<Map<String, String>> consumer) throws IOException {
        DatagramPacket datagramPacket = SSDP.createRequest(string, n, string2, n2);
        MulticastSocket multicastSocket = new MulticastSocket(datagramPacket.getPort());
        try {
            multicastSocket.setSoTimeout(n2 * 1000);
            multicastSocket.send(datagramPacket);
            byte[] byArray = new byte[2048];
            DatagramPacket datagramPacket2 = new DatagramPacket(byArray, byArray.length);
            try {
                while (true) {
                    multicastSocket.receive(datagramPacket2);
                    consumer.accept(SSDP.parseResponse(datagramPacket2.getData()));
                }
            }
            catch (SocketTimeoutException socketTimeoutException) {
                multicastSocket.close();
            }
        }
        catch (Throwable throwable) {
            try {
                multicastSocket.close();
            }
            catch (Throwable throwable2) {
                throwable.addSuppressed(throwable2);
            }
            throw throwable;
        }
    }
}

