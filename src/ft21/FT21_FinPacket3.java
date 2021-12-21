package ft21;

public class FT21_FinPacket3 extends FT21Packet {

    public final int seqN;
    public final int time;

    public FT21_FinPacket3(int seqN, int packetID, int time) {
        super(PacketType.FIN);
        super.putInt( seqN );
        super.putByte( Integer.BYTES * 2);
        super.putInt(packetID);
        super.putInt(time);
        this.seqN = seqN;
        this.time = time;
    }

    public String toString() {
        return String.format("FIN<%d>", seqN);
    }
}
