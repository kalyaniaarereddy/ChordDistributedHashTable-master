import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;


public class JavaServer{

	public static CalculatorHandler handler;
	public static FileStore.Processor processor;
	public static int port;
	
	public static void main(String[] args) {

	try {
      		port= Integer.valueOf(args[0]);
		handler = new CalculatorHandler(port);
      		processor = new FileStore.Processor(handler);
     		Runnable simple = new Runnable() {
        	public void run() {
          		simple(processor);
        	}
      		};      
     
      		new Thread(simple).start();
    } catch (Exception x) 
    {
    	x.printStackTrace();
    }

	}


 public static void simple(FileStore.Processor processor) {
    try {
	
      TServerTransport serverTransport = new TServerSocket(port);
      //TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));
      TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
      System.out.println("Starting the simple server...");
      server.serve();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}