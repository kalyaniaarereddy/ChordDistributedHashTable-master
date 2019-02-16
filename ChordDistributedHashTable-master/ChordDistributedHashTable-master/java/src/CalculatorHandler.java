import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Iterator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class CalculatorHandler implements FileStore.Iface {
	public String port="";
	public String ip="";
	public String hash="";
	public NodeID node=null;
	public static List<NodeID> nodes=new ArrayList<NodeID>();


	public CalculatorHandler(int Port) {
        	this.port=String.valueOf(Port);
		this.hash="";
		try{
			InetAddress inet=InetAddress.getLocalHost();
			this.ip=inet.getHostAddress();
			String tog=this.ip+":"+this.port;
			this.hash=sha256(tog);
		}catch(NoSuchAlgorithmException e)
		{
			System.out.println("in exception");
		}
		catch(UnknownHostException e)
		{
			System.out.println("unknown host");
		}
		node = new NodeID(this.hash,this.ip,Port);
	}

	
	public void writeFile(RFile rfile) throws SystemException
	{
		//RFileMetadata meta =new RFileMetadata();
		//rfile.meta=meta;
		
		try{
			String tog3=rfile.meta.owner+":"+rfile.meta.filename;
			rfile.meta.contentHash=sha256(tog3);
		}catch(NoSuchAlgorithmException e)
		{
			System.out.println("in exception");
		}

		rfile.meta.version=0;
		NodeID write_node = findSucc(rfile.meta.contentHash);			
		String f=rfile.meta.filename;
		File file=new File(f);
		if(file.exists())
		{
			rfile.meta.version=rfile.meta.version+1;
		}
		FileWriter fw=null;
		BufferedWriter bw=null;

		try{
			fw=new FileWriter(file);
			bw=new BufferedWriter(fw);
			bw.write("filename:"+f+"\n");
			bw.write("owner:"+rfile.meta.owner+"\n");
			bw.write("version:"+rfile.meta.version+"\n");
			bw.write("content hash:"+rfile.meta.contentHash+"\n");
			bw.write("content:"+rfile.content+"\n");
			bw.close();
		} 
		catch (FileNotFoundException e) {
			System.err.println(e);		
		}
		catch (IOException e) {
			System.err.println(e);		
		}
		finally
		{
			try{
			if(bw!=null)
				bw.close();
			}catch(IOException e)
			{
				System.out.println(e);
			}
		}
	}

	static String sha256(String input) throws NoSuchAlgorithmException 
	{
        MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
         
        return sb.toString();
    }


	public RFile readFile(String filename, String owner) throws SystemException
	{
		String key="";
		try{
		 key= sha256(owner + ":" + filename);
		}catch(NoSuchAlgorithmException e)
		{
			System.out.println(e);
		}	
		NodeID node = findSucc(key);
		RFile rfile = new RFile();
		RFileMetadata meta =new RFileMetadata();
		rfile.meta=meta;
		File f=new File(filename);
		if(!f.exists())
		{
			System.out.println("File does not exist at the node id");
			return rfile;
		}
		
		FileReader fr=null;
		BufferedReader br=null;
		String Line;

		try{
			fr=new FileReader(filename);
			br=new BufferedReader(fr);
			while((Line=br.readLine())!=null)
			{
				if(Line.toLowerCase().contains("filename:"))
				{
					rfile.meta.filename=Line.substring(Line.indexOf(":")+1);
				}
				else if(Line.toLowerCase().contains("owner:"))
				{
					rfile.meta.owner=Line.substring(Line.indexOf(":")+1);
				}
				else if(Line.toLowerCase().contains("version:"))
				{
					rfile.meta.version=Integer.parseInt(Line.substring(Line.indexOf(":")+1));
				}
				else if(Line.toLowerCase().contains("content hash:"))
				{
					rfile.meta.contentHash=Line.substring(Line.indexOf(":")+1);
				}
				else if(Line.toLowerCase().contains("content:"))
				{
					rfile.content=Line.substring(Line.indexOf(":")+1);
				}
			}

			br.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println(e);		
		} 
		catch(IOException e)
		{
			System.out.println(e);		
		} 	
		finally
		{
			try{
			if(br!=null)
				br.close();
			}catch(IOException e)
			{
				System.out.println(e);		
			} 
		}

		return rfile;
	}

	
	public void setFingertable(List<NodeID> node_list)
	{
		nodes.addAll(node_list);
	}

	
	public NodeID findSucc(String key) throws SystemException
	{
	NodeID node = findPred(key);
	TTransport transport=null;
	FileStore.Client client=null;
	try {
    	transport = new TSocket(node.ip,node.port);
        transport.open();
   	TProtocol protocol = new  TBinaryProtocol(transport);
      	client = new FileStore.Client(protocol);
    	} catch (TException x) {
    		x.printStackTrace();
    	} 
	
	NodeID node_succ=null;
	try{
		node_succ=client.getNodeSucc();
	}catch(TException e)
	{
		System.out.println(e);
	}
	return node_succ;
	}

	public NodeID findPred(String key) throws SystemException
	{
		NodeID n1 = node;
		String tog=n1.ip+":"+n1.port;
		String node_hash="";

		try{
			node_hash=sha256(tog);
		}catch(NoSuchAlgorithmException e)
		{
			System.out.println(e);
		}

		//to get just first row in finger table
		Iterator itr=nodes.iterator();  
		NodeID st=(NodeID)itr.next(); 


		if(node_hash.compareTo(st.id) >= node_hash.compareTo(key))
		{
		//in this case the file should be stored at node n1

			return n1;

		}

		else if(st.id.compareTo(node_hash) > st.id.compareTo(key))
		{
		//in this case next node should be searched
			

			boolean b=true;
			int counter=0;
			do
			{
				NodeID node_pred=null;
				if(counter==0)
				{
					node_pred=closest_preceding_finger(key,n1);
					counter++;
				}
				else
				{
					node_pred=closest_preceding_finger(key,node_pred);
					String tog2=node_pred.ip+":"+node_pred.port;
					String node_hash2="";
					try{
						node_hash2=sha256(tog2);
					}catch(NoSuchAlgorithmException e)
					{
						System.out.println("in exception");
					}

					Iterator itr2=nodes.iterator();  
					NodeID st2=(NodeID)itr2.next(); 
					if(node_hash2.compareTo(st2.id) <= node_hash2.compareTo(key))
					{
						return node_pred;
					}
					else
					{
						b=true;
					}
				}

			}while(b);
			
		}	//end of else-if

		return n1;
	}


	NodeID closest_preceding_finger(String key,NodeID n)
	{
		
		String tog=n.ip+":"+n.port;
		String node_hash="";
		try{
			node_hash=sha256(tog);
		}catch(NoSuchAlgorithmException e)
		{
			System.out.println("in exception");
		}

		int m;
		for(m=255;m>=0;m--)
		{
			if(node_hash.compareTo(key) >= node_hash.compareTo(nodes.get(m).id))
			{
				return nodes.get(m);
			}
	
		}
		return n;

	}

	
	public NodeID getNodeSucc() throws SystemException
	{
		return nodes.get(0);
	}

	
}