package ft21;

public class FT21_DataPacket2 extends FT21Packet {
    public final int seqN;
    public final byte[] data;

    public FT21_DataPacket2(int seqN, byte[] data) {
        this(seqN, data, data.length);
    }

    public FT21_DataPacket2(int seqN, byte[] data, int datalen) {
        super(PacketType.DATA);
        super.putInt(seqN);
        super.putByte(4);
        super.putInt(seqN);
        super.putBytes(data, datalen);
        this.seqN = seqN;
        this.data = data;
    }


    public String toString() {
        return String.format("DATA<%d, len: %d>", seqN, data.length);
    }

}