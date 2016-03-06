package se.sics.jipv6.core;

import java.util.Arrays;
import java.util.HashMap;

public class LoWPANFragmenter {

    class FragmentContext {
        int tag;
        int size;
        int receivedSize;
        long time;
        byte[] data;
        int firstFragmentOffset = 0;
        HashMap <String, String> map = new HashMap<String, String>();
        
        FragmentContext(int tag, int size) {
            data = new byte[size];
            this.tag = tag;
            this.time = System.currentTimeMillis();
            this.size = size;
            this.receivedSize = 0;
        }

        /* copy whole packet payload to buffer */
        public boolean copyData(IPv6Packet packet, int offset) {
            int plen = packet.getPayloadLength();
            if (map.get("" + offset + ":" + plen) == null) {
                if(offset > 0) {
                    /* compensate for this offset */
                    offset = offset - firstFragmentOffset;
                }
                packet.copy(0, data, offset);
                map.put("" + offset + ":" + plen, "");
                return true;
            } else {
                System.out.println("*** already received that part");
                return false;
            }
        }

        public void setPacketPayload(IPv6Packet packet) {
            byte[] cleanedData = Arrays.copyOf(data, data.length - firstFragmentOffset);
            packet.setBytePayload(cleanedData);
        }
    }
    
    private HashMap<String, FragmentContext> fragmentMap = new HashMap<String, FragmentContext>();

    public boolean handleFragment(IPv6Packet packet, int uncomprSize, int comprSize) {
        int data = packet.getData(0);

        if ((data & 0xf8) == HC06Packeter.SICSLOWPAN_DISPATCH_FRAG1) {
            /* This is a first fragment */
            int fragSize = packet.get16(0) & 0x7ff;
            int fragTag = packet.get16(2);
            String id = packet.getLinkDestinationAsString() + "-" + fragTag;
            System.out.printf("First Fragment found: size:%d tag:%d ID:%s\n", fragSize, fragTag, id);
            packet.incPos(4);
            
            FragmentContext ctx = fragmentMap.get(id);
            if (ctx == null) {
                ctx = new FragmentContext(fragTag, fragSize);
                fragmentMap.put(id, ctx);
            }
            if (ctx.copyData(packet, 0)) {
                ctx.receivedSize += packet.getPayloadLength() + (uncomprSize - comprSize);
                ctx.firstFragmentOffset = (uncomprSize - comprSize);
            }
            System.out.println("Received:" + ctx.receivedSize);
            /* Fragment 1 should never be "complete" since it would not be fragmentet then? */
            return false;
        } else if ((data & 0xf8) == HC06Packeter.SICSLOWPAN_DISPATCH_FRAGN) {
            int fragSize = packet.get16(0) & 0x7ff;
            int fragTag = packet.get16(2);    
            int fragOffset = packet.getData(4) * 8;
            String id = packet.getLinkDestinationAsString() + "-" + fragTag;
            System.out.printf("N Fragment found: size:%d tag:%d offset:%d ID:%s\n", fragSize, fragTag, fragOffset, id);
            packet.incPos(5);
            /* here we should handle appending the packet data to the fragment buffers... */
            FragmentContext ctx = fragmentMap.get(id);
            if (ctx == null) {
                ctx = new FragmentContext(fragTag, fragSize);
                fragmentMap.put(id, ctx);
            }
            /* typically no extra header size here? */
            if (ctx.copyData(packet, fragOffset)) {
                ctx.receivedSize += packet.getPayloadLength();
            }
            System.out.println("Received:" + ctx.receivedSize);
            if (ctx.receivedSize == ctx.size) {
                System.out.println("**** Packet done !!!!");
                packet.currentPos -= 5; /* back down to regular 802.15.4 header - then put a "big" packet there... */
                ctx.setPacketPayload(packet);
                return true;
            }
            return false;
        }
        return true;
    }
    
}
