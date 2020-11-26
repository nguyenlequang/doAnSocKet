import java.io.*;
import java.net.*;
import java.util.*;
public class Proxy extends Thread {

	public static void main(String[] args) {
		Proxy myProxy=new Proxy(8888); // lắng nghe client ở cổng 8888
		
		myProxy.listen();  // gọi method listen() ở dưới

	}
	
	private ServerSocket serverSocket;  // tạo ra 1 cái socket 
	
	private volatile boolean running=true; // điều kiện để tiếp tục chạy hàm run() ở phía dưới
	
	// vì HashMap là 1 collection nên nó được serialize (tuần tự) nên có thể đọc ghi dữ liệu theo kiểu ObjectOut/InputStream
	static HashMap<String,String>blackList;  //danh sách các website sẽ bị block
	
	static ArrayList<Thread>receivingThread;  // danh sách các luồng dữ liệu trong chương trình 
	
	public Proxy(int port) { // constructor khi khởi tạo 1 object proxy với tham số cần truyền vào là cổng để kết nối với client
		blackList=new HashMap<String,String>(); // khởi tạo blackList là 1 collection framework kiểu HashMap (key-value)
		receivingThread=new ArrayList<Thread>(); // Khởi tạo mảng các luồng dữ liệu trong chương trình
		
		new Thread(this).start(); // gọi start() thì hàm start() sẽ gọi đến hàm run(), đây là 1 hàm đã được định nghĩa sẵn trong Thead class
		
		try {
			
			File blackListFile =new File("blackList.conf"); // cố để mở file "backList.conf" 
			
			if(!blackListFile.exists()) { // nếu file không tồn tại
				System.out.println("Can not find any File like this!!!");
				blackListFile.createNewFile(); // tạo ra 1 file mới có tên "blackList.conf" trong folder
			}
			else {
				// nếu file đã tồn tại trong folder
				ObjectInputStream objectInputStream=new ObjectInputStream(new FileInputStream(blackListFile)); // đọc từ file backListFile theo kiểu object
				blackList=(HashMap<String,String>)objectInputStream.readObject(); // ép kiểu dữ liệu về HashMap
				objectInputStream.close();
				// kiểu ObjectInputStream là ta sẽ đọc dữ liệu theo kiểu mà ta đã lưu dữ liệu vào file
			}
		}
		catch(Exception e) // nếu xảy ra lỗi thì sẽ chạy vào exception
		{
			System.out.println(e);
		}
		
		// kết nối với client 
		
		try {
			serverSocket=new ServerSocket(port); // tạo ra 1 socket tại cổng port (8888) - đây là cổng mà proxy lắng nghe clienrt
			System.out.println("Waiting for client on port "+serverSocket.getLocalPort()+" ...");
			this.running=true; // gán cho running=true để chương trình tiếp tục chạy
		}
		catch(Exception e) {
			System.out.println(e);
		}
		
	}
	
	// hàm lắng nghe và thực hiện lệnh được gọi trong hàm main
	public void listen() {
		while(running) { // chạy khi running =true,khi false thì nó sẽ dừng
			try {
				
				Socket socket=serverSocket.accept(); // tạo ra 1 socket để kết nối vs serverSocket(proxy) tại cổng 8888
				// nói sau hàm này
				Thread thread=new Thread(new RequestHandler(socket)); // đây là class RequestHandler.java
				thread.start(); // thread sẽ chạy hàm run() trong class RequestHandler.java
				
				receivingThread.add(thread); // thêm thread vào mảng các thread array của chương trình
			}
			catch(Exception e) {
				System.out.println(e);
			}
		}
	}
	
	private void closeServer() {
		System.out.println("\nClosing server.......");
		try
		{			
			FileOutputStream fileOut=new FileOutputStream("blackList.conf");
			ObjectOutputStream obj=new ObjectOutputStream(fileOut);
			obj.writeObject(blackList);// ghi nguyên 1 object vào với kiểu dữ liệu HashMap<String,String>
			obj.close();
			fileOut.close();
			System.out.println("Black list is saved!!");
		}
		catch(Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		
		try {
			for(Thread thread :receivingThread) {
				if(thread.isAlive())
				{
					System.out.println("waiting for "+thread.getId()+" to close");
					thread.join();
					System.out.println("closed");
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		
		try{
			System.out.println("Terminating Connection");
			serverSocket.close();
		} catch (Exception e) {
			System.out.println("Exception closing proxy's server socket");
			e.printStackTrace();
		}
	}
	
	
	public static boolean isBlocked(String url)
	{
		if(blackList.get(url)!=null)
			return true;
		return false;
	}
	
	@Override
	public void run()
	{
		Scanner scanner=new Scanner(System.in);
		String command;
		while(running)
		{
			System.out.println("Enter new Site to block");
			System.out.println("or Enter \"blocked\" to see blackList");
			System.out.println("or Enter \"close\" to close the server");
			command=scanner.nextLine();
			if(command.toLowerCase().equals("blocked")) {
				System.out.println("The current blackList is :");
				for(String key : blackList.keySet()) {
					System.out.println(key);
				}
				System.out.println();
			}
			
			else if(command.toLowerCase().equals("close")) {
				running=false;
				closeServer();
			}
			else
			{
				blackList.put(command, command);
				System.out.println("Saved Successfully");
			}
		}
		scanner.close();
	}
}
