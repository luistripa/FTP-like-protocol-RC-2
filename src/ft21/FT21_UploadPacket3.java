package ft21;

public class FT21_UploadPacket3 extends FT21Packet {

    public final String filename;
    public final int time;

    public FT21_UploadPacket3(String filename, int packetID, int time) {
        super(PacketType.UPLOAD);
        super.putByte(Integer.BYTES * 2);
        super.putInt(packetID);
        super.putInt(time);
        this.filename = filename;
        super.putString(filename);
        this.time = time;
    }

    public String toString() {
        return String.format("UPLOAD<%s>", filename);
    }

}
