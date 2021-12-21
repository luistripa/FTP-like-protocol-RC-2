package ft21;

public class FT21_DataPacket3 extends FT21Packet {

    public final int seqN;
    public final byte[] data;

    public FT21_DataPacket3(int seqN, byte[] data, int packetID, int time) {
        this(seqN, data, data.length, packetID, time);
    }

    public FT21_DataPacket3(int seqN, byte[] data, int datalen, int packetID, int time) {
        super(FT21Packet.PacketType.DATA);
        super.putInt(seqN);
        super.putByte(Integer.BYTES * 2);
        super.putInt(packetID);
        super.putInt(time);
        super.putBytes(data, datalen);
        this.seqN = seqN;
        this.data = data;
    }

    public String toString() {
        return String.format("DATA<%d, len: %d>", seqN, data.length);
    }

}
