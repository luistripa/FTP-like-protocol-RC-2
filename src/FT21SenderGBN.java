import cnss.simulator.Node;
import ft21.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.LinkedList;

public class FT21SenderGBN extends FT21AbstractSenderApplication {

    static class PacketProxy {

        int id;
        FT21Packet packet;
        int timestamp;

        public PacketProxy(int id, FT21Packet packet, int timestamp) {
            this.id = id;
            this.packet = packet;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "PacketProxy{" +
                    "id=" + id +
                    ", packet=" + packet +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    private static final int TIMEOUT = 1000;

    static int RECEIVER = 1;

    enum State {
        BEGINNING, UPLOADING, FINISHING, FINISHED
    };

    static int DEFAULT_TIMEOUT = 1000;

    private File file;
    private RandomAccessFile raf;
    private int BlockSize;
    private int nextPacketSeqN, lastPacketSeqN;
    private int maxWindowSize;

    private State state;
    private LinkedList<PacketProxy> window;

    public FT21SenderGBN() {
        super(true, "FT21SenderGBN");
    }

    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        super.initialise(now, node_id, nodeObj, args);

        raf = null;
        file = new File(args[0]);
        BlockSize = Integer.parseInt(args[1]);
        maxWindowSize = Integer.parseInt(args[2]);

        state = State.BEGINNING;
        lastPacketSeqN = (int) Math.ceil(file.length() / (double) BlockSize);

        window = new LinkedList<>();
        return 1;
    }

    public void on_clock_tick(int now) {
        if (state != State.FINISHED) {
            if (!window.isEmpty() && (now - window.getFirst().timestamp) > TIMEOUT) {
                PacketProxy packetProxy = window.getFirst();
                window.clear();
                window.addLast(packetProxy);
                super.sendPacket(now, RECEIVER, packetProxy.packet);
                if (packetProxy.id == lastPacketSeqN + 1) {
                    state = State.FINISHING;
                }
                else {state = State.UPLOADING;}
            }
            else if (window.size() < maxWindowSize) {
                sendNextPacket(now);
            }
        }
    }

    private void sendNextPacket(int now) {
        PacketProxy packetProxy;
        switch (state) {
            case BEGINNING:
                packetProxy = new PacketProxy(0,new FT21_UploadPacket(file.getName()), now);
                window.addLast(packetProxy);
                super.sendPacket(now, RECEIVER, packetProxy.packet);
                state = State.UPLOADING;
                break;
            case UPLOADING:
                int nextId = window.getLast().id + 1;
                packetProxy = new PacketProxy(nextId,readDataPacket(file, nextId), now);
                window.addLast(packetProxy);
                super.sendPacket(now, RECEIVER, packetProxy.packet);
                if(nextId > lastPacketSeqN) {
                    state = State.FINISHING;
                }
                break;
            case FINISHING:
                if (window.isEmpty()) {
                    packetProxy = new PacketProxy(lastPacketSeqN + 1, new FT21_FinPacket(lastPacketSeqN + 1), now);
                    window.addLast(packetProxy);
                    super.sendPacket(now, RECEIVER, packetProxy.packet);
                }
                break;
            case FINISHED:
        }
    }

    @Override
    public void on_receive_ack(int now, int client, FT21_AckPacket ack) {
        switch (state) {
            case BEGINNING:
                break;
            case UPLOADING:
                if(ack.cSeqN == window.getFirst().id)
                    window.poll();
                break;
            case FINISHING:
                super.log(now, "All Done. Transfer complete...");
                super.printReport(now);
                if(ack.cSeqN == window.getFirst().id) {
                    window.poll();
                    if(ack.cSeqN == lastPacketSeqN + 1)
                       state = State.FINISHED;
                }
                break;
            case FINISHED:
        }
    }

    private FT21_DataPacket readDataPacket(File file, int seqN) {
        try {
            if (raf == null)
                raf = new RandomAccessFile(file, "r");

            raf.seek(BlockSize * (seqN - 1));
            byte[] data = new byte[BlockSize];
            int nbytes = raf.read(data);
            return new FT21_DataPacket(seqN, data, nbytes);
        } catch (Exception x) {
            throw new Error("Fatal Error: " + x.getMessage());
        }
    }
}
