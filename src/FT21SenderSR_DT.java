import ft21.FT21AbstractSenderApplication;
import cnss.simulator.Node;
import ft21.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.LinkedList;

public class FT21SenderSR_DT extends FT21AbstractSenderApplication {

    static class PacketProxy {

        FT21Packet packet;
        int id;
        int timestamp;
        boolean acknowledged;

        public PacketProxy(int id, FT21Packet packet, int timestamp) {
            this.id = id;
            this.packet = packet;
            this.timestamp = timestamp;
            this.acknowledged = false;
        }

        @Override
        public String toString() {
            return "PacketProxy{" +
                    "packet=" + packet +
                    ", id=" + id +
                    ", timestamp=" + timestamp +
                    ", acknowledged=" + acknowledged +
                    '}';
        }
    }

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
    private int timeout;
    private int maxTimeout, minTimeout;

    private State state;
    private LinkedList<PacketProxy> window;

    public FT21SenderSR_DT() {
        super(true, "FT21SenderSR_DT");
        this.timeout = DEFAULT_TIMEOUT;
    }

    public int initialise(int now, int node_id, Node nodeObj, String[] args) {
        super.initialise(now, node_id, nodeObj, args);

        raf = null;
        file = new File(args[0]);
        BlockSize = Integer.parseInt(args[1]);
        maxWindowSize = Integer.parseInt(args[2]);

        state = State.BEGINNING;
        nextPacketSeqN = 0;
        lastPacketSeqN = (int) Math.ceil(file.length() / (double) BlockSize);

        window = new LinkedList<>();
        return 1;
    }

    public void on_clock_tick(int now) {
        if (state != State.FINISHED) {
            boolean timedout = false;
            for (PacketProxy p : window) {
                if (p.acknowledged) // Ignore already acknowledged packets
                    continue;
                if (now - p.timestamp > timeout) { // When packet enters TIMEOUT
                    this.on_timeout(now); // Used for statistics only
                    p.timestamp = now;
                    super.sendPacket(now, RECEIVER, p.packet);
                    timedout = true;
                    break;
                }
            }
            if (!timedout && window.size() < maxWindowSize) {
                sendNextPacket(now);
            }
        }
    }

    private void sendNextPacket(int now) {
        PacketProxy packetProxy;
        switch (state) {
            case BEGINNING:
                if (nextPacketSeqN == 0) {
                    packetProxy = new PacketProxy(0,new FT21_UploadPacket3(file.getName(), 0, now), now);
                    window.addLast(packetProxy);
                    super.sendPacket(now, RECEIVER, packetProxy.packet);
                    nextPacketSeqN++;
                }
                break;
            case UPLOADING:
                if(nextPacketSeqN <= lastPacketSeqN) {
                    packetProxy = new PacketProxy(nextPacketSeqN, readDataPacket(file, nextPacketSeqN, now), now);
                    window.addLast(packetProxy);
                    super.sendPacket(now, RECEIVER, packetProxy.packet);
                    nextPacketSeqN++;
                }
                break;
            case FINISHING:
                if (nextPacketSeqN == lastPacketSeqN+1) {
                    packetProxy = new PacketProxy(nextPacketSeqN,
                            new FT21_FinPacket3(nextPacketSeqN, lastPacketSeqN + 1, now), now);
                    window.addLast(packetProxy);
                    super.sendPacket(now, RECEIVER, packetProxy.packet);
                    nextPacketSeqN++;
                }
                break;
            case FINISHED:
        }
    }

    @Override
    public void on_receive_ack(int now, int client, FT21_AckPacket ack) {
        super.logPacket(now, ack);
        int rtt = now - ack.time;
        super.tallyRTT(rtt);
        super.tallyTimeout(timeout);
        timeout = rtt;

        switch (state) {
            case BEGINNING:
                window.poll();
                state = State.UPLOADING;
                break;

            case UPLOADING:
                if (ack.cSeqN == lastPacketSeqN) {
                    this.window.clear();
                    state = State.FINISHING;

                } else if (ack.cSeqN >= window.getFirst().id) {
                    while (!window.isEmpty() && (window.getFirst().id <= ack.cSeqN || window.getFirst().acknowledged))
                        window.poll();

                } else if (windowContains(ack.packetID) && !ack.outsideWindow) {
                    PacketProxy packetProxy = getPacket(ack.packetID);
                    packetProxy.acknowledged = true;
                }
                break;

            case FINISHING:
                if(ack.cSeqN == lastPacketSeqN + 1) {
                    window.poll();
                    state = State.FINISHED;
                    super.log(now, "All Done. Transfer complete...");
                    super.printReport(now);
                }
                break;

            case FINISHED:
        }
    }

    private PacketProxy getPacket(int packetID) {
        PacketProxy first = window.getFirst();
        int x = packetID - first.id;
        return window.get(x);
    }

    private boolean windowContains(int packetID) {
        PacketProxy last = window.getLast();
        PacketProxy first = window.getFirst();
        return packetID <= last.id && packetID >= first.id;
    }

    private FT21_DataPacket3 readDataPacket(File file, int seqN, int now) {
        try {
            if (raf == null)
                raf = new RandomAccessFile(file, "r");

            raf.seek(BlockSize * (seqN - 1));
            byte[] data = new byte[BlockSize];
            int nbytes = raf.read(data);
            return new FT21_DataPacket3(seqN, data, nbytes, seqN, now);
        } catch (Exception x) {
            throw new Error("Fatal Error: " + x.getMessage());
        }
    }
}
