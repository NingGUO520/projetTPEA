package projet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONObject;

public class Politicien implements Runnable{
	
	public static int SIZE_ByteBuffer = 500000;
	final static Charset charset = Charset.forName("UTF-8");
    protected ByteBuffer bb = ByteBuffer.allocate(SIZE_ByteBuffer);
    protected SocketChannel socketChannel;
    private boolean isConnected;
    protected String name = this.getClass().getSimpleName() + " : ";
    ReentrantLock lock = new ReentrantLock();
    
    private Socket socket;
    private DataOutputStream outchan;
	private BufferedReader inchan;
	
	public Politicien(String url, int port) throws IOException {
//		SocketAddress sa = new InetSocketAddress(url, port);
//        socketChannel = SocketChannel.open();
//        socketChannel.connect(sa);
//        isConnected = true;
        
		socket = new Socket(url, port);
        inchan = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outchan = new DataOutputStream(socket.getOutputStream());
		
		isConnected = true;
		System.out.println("POLITICIEN CONNECTED");
        System.out.println("------------------------");
	}


	@Override
	public void run() {
		JSONObject obj = new JSONObject();
		obj.put("listen", "null");
		String msg = obj.toString();
		long taille = msg.length();
		try {
			outchan.writeLong(taille);
			outchan.writeUTF(msg);
			outchan.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (isConnected) {
				String content = br.readLine();
				System.out.println(content);
            }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//        try {
//        	System.out.println();
//        	ByteBuffer bs = charset.encode(msg);
//            bb.put(bs);
//            bb.flip();
//			socketChannel.write(bb);
//			bb.clear();
//			int n;
//			while (isConnected && (n = socketChannel.read(bb)) >= 0) {
//				System.out.println("Recived");
//				bb.flip();
//	            int id = bb.get();
//	            this.readString();
//	            System.out.println("Recived Message From  <--------- " + socketChannel.getRemoteAddress()
//	                    + " \n\t " + id);
//	            bb.clear();
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	public String readString() {
        int n = bb.getInt();
        int lim = bb.limit();
        bb.limit(bb.position() + n);
        String s = charset.decode(bb).toString();
        bb.limit(lim);
        return s;
    }

}
