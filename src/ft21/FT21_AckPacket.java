package ft21;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class FT21_AckPacket extends FT21Packet {
	public final int cSeqN;
	public final boolean outsideWindow;
	public int packetID;
	public int time;
	
	FT21_AckPacket(byte[] bytes) {
		super( bytes );
		int seqN = super.getInt();
		this.cSeqN = Math.abs( seqN );
		this.outsideWindow = seqN < 0;

		try {
			this.packetID = super.getInt();
		} catch (BufferUnderflowException e) {
			this.packetID = 0;
		}

		// decode optional fields here...
		try {
			this.time = super.getInt();
		} catch (BufferUnderflowException e) {
			this.time = 0;
		}
	}

	public String toString() {
		if (this.packetID == 0)
			return String.format("ACK<%d>", cSeqN);
		else
			return String.format("ACK<%d - %d>", cSeqN, packetID);
	}
	
}