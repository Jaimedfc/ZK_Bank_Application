package es.upm.dit.dscc.actreplica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper; 
import org.apache.zookeeper.data.Stat;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class zGroup implements Watcher {
	
	private static final int SESSION_TIMEOUT = 5000;

	private static String rootMembers = "/members";
	private static String aMember = "/member-";
	private static String requests = "/req";
	private static String aRequest = "/solicitud-";
	private static String cmd = "/cmd";
	private static String cnt = "/cnt";
	private static String command = "/command";
	private String myId;
	public boolean isLeader = false;
	private boolean noReq = true;
	private List<String> prevView;
	
	// This is static. A list of zookeeper can be provided for decide where to connect
	// Indicar las IPs de los equipos que se piensa usar.
	String[] hosts = {"138.4.31.58:2181", "138.4.31.59:2181"};

	private ZooKeeper zk;
	
	// Metodo que obtiene la IP del equipo en la interfaz que se indica
	public byte[] getIP() throws IOException {
		try {
			//Modificar la interfaz en funcion de la que se utilice
			NetworkInterface ni = NetworkInterface.getByName("enp1s0");
			String ip = ni.getInterfaceAddresses().get(1).toString();
			ip = ip.split(" ")[0].split("/")[1];
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(out);
			os.writeObject(ip);
			return out.toByteArray();
		} catch(SocketException e) {
			System.out.println("Couldn't get the IP");
			throw new RuntimeException(e);
		}
	}
	
	//Metodo que indica si se es lider o no
	public boolean getIsLeader() {
		return isLeader;
	}
	
	//Devuelve la IP del ultimo equipo en conectarse
	public String getNewIp() {
		byte[] addr;
		try {
			Stat s = zk.exists(rootMembers+"/"+getView().get(getView().size()-1), false);
			addr = zk.getData(rootMembers+"/"+getView().get(getView().size()-1), false, s);
			ByteArrayInputStream in = new ByteArrayInputStream(addr);
			ObjectInputStream is = new ObjectInputStream(in);
			String dir = (String) is.readObject();
			return dir;
		}catch(Exception e) {
			System.out.println("Couldn't get the IP of the new bank");
		}
		return null;
	}
	
	public zGroup () throws IOException {
		Random rand = new Random();
		int i = rand.nextInt(hosts.length);
		try {
			if (zk == null) {
				zk = new ZooKeeper(hosts[i], SESSION_TIMEOUT, cWatcher);
				try {
					// Wait for creating the session. Use the object lock
					wait();
				} catch (Exception e) {
					System.out.println("Couldn't create the zk session.");
				}
			}
		} catch (Exception e) {
			System.out.println("Error");
		}

		// Add the process to the members in zookeeper

		if (zk != null) {
			// Create a folder for members and include this process/server
			try {
				// Create a folder, if it is not created
				Stat a = zk.exists(rootMembers, watcherMember);
				if (a == null) {
					// Created the znode, if it is not created.
					zk.create(rootMembers, new byte[0], 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				Stat b = zk.exists(requests, watcherRequests);
				if (b == null) {
					// Created the znode, if it is not created.
					zk.create(requests, new byte[0], 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				Stat c = zk.exists(cmd, watcherCmd);
				if (c == null) {
					// Created the znode, if it is not created.
					zk.create(cmd, new byte[0], 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				Stat d = zk.exists(cmd+cnt, watcherCnt);
				if (d == null) {
					// Created the znode, if it is not created.
					zk.create(cmd+cnt, ByteBuffer.allocate(4).putInt(0).array(), 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				Stat e = zk.exists(cmd+command, watcherCommand);
				if (e == null) {
					// Created the znode, if it is not created.
					zk.create(cmd+command, new byte[0], 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				
				// Create a znode for registering as member and get my id
				myId = zk.create(rootMembers + aMember, getIP(), 
						Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
				myId = myId.replace(rootMembers + "/", "");

				List<String> list = zk.getChildren(rootMembers, watcherMember, a);
				prevView=list;
				Collections.sort(list);
				printListMembers(list);
				if(myId.equals(list.get(0))) {
					isLeader=true;
					System.out.println("Soy líder");} 
				else {System.out.println("No soy líder");}
				if(!isLeader) {receiveDB();}
				getRequests();
			} catch (KeeperException e) {
				System.out.println("The session with Zookeeper failes. Closing");
				return;
			} catch (InterruptedException e) {
				System.out.println("InterruptedException raised");
			}

		}
	}
	
	private void printListMembers (List<String> list) {
		System.out.println("Remaining # members:" + list.size());
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			System.out.print(string + ", ");				
		}
		System.out.println();
	}
	
	//Metodo que devuelve los hijos de un nodo ordenados
	public List<String> getChildren (String path, Watcher watcher) {
		List<String> list = new ArrayList<String>();
		try {
			list = zk.getChildren(path, watcher);
			Collections.sort(list);
			return list;
		} catch (KeeperException e) {
			System.out.println("The session with Zookeeper failes. Closing");
		} catch (InterruptedException e) {
			System.out.println("InterruptedException raised");
		}
		return list;
	}
	
	//Metodo que utiliza getChildren para obtener una vista de los miembros
	public List<String> getView() {
		return getChildren(rootMembers, watcherMember);
	}
	
	//Metodo que utiliza getChildren para obtener una lista de las peticiones ordenadas
	public List<String> getRequests() {
		return getChildren(requests, watcherRequests);
	}
	

	// Notified when the session is created
	private Watcher cWatcher = new Watcher() {
		public void process (WatchedEvent e) {
			System.out.println("Created session");
			System.out.println(e.toString());
			notify();
		}
	};

	// Notified when the number of children in /member is updated
	private Watcher  watcherMember = new Watcher() {
		public void process(WatchedEvent event) {
			try {
				List<String> list = getView();
				printListMembers(list);
				if(myId.equals(list.get(0))) {
					isLeader=true;
					System.out.println("Soy líder");
				} else {
					isLeader = false;
					System.out.println("No soy líder");
				}
				if (isLeader && (list.size()>prevView.size())) {
					String newIP = getNewIp();
					sendDB(newIP);
				}
				prevView = list;
			} catch (Exception e) {
				System.out.println("Exception: wacherMember");
			}
		}
	};
	//Watcher que notifica cuando se modifica la lista de los hijos del nodo /req
	private Watcher  watcherRequests = new Watcher() {
		public void process(WatchedEvent event) {
			try {
				zk.exists(requests,watcherRequests);
				List<String> list = getRequests();
				if (isLeader && noReq) {
					Stat exists2 = zk.exists(cmd+command, watcherCommand);
					noReq=false;
					Stat s = zk.exists(requests+"/"+list.get(0), false);
					byte[] op = zk.getData(requests+"/"+list.get(0),false,s);
					zk.setData(cmd+command, op, exists2.getVersion());
					deleteNode(requests+"/"+list.get(0));
				}
			} catch (Exception e) {
				System.out.println("Exception: wacherRequests");
			}
		}
	};
	
	//Watcher que notifica cuando se modifica el nodo /cmd (No se usa para dar funcionalidad)
	private Watcher  watcherCmd = new Watcher() {
		public void process(WatchedEvent event) {	
			try {
			} catch (Exception e) {
				System.out.println("Exception: wacherCmd");
			}
		}
	};
	
	//Watcher que notifica cuando se modifica el nodo /cmd/cnt (Un contador de ejecucion)
	private Watcher  watcherCnt = new Watcher() {
		public void process(WatchedEvent event) {
			try {
				if(isLeader) {
					Stat exists = zk.exists(cmd+cnt, false);
					Stat exists2 = zk.exists(cmd+command, false);
					byte[] counter = zk.getData(cmd+cnt,watcherCnt,exists);
					int count = ByteBuffer.wrap(counter).getInt();
					if (count>=getView().size()) {
						zk.setData(cmd+cnt, ByteBuffer.allocate(4).putInt(0).array(), exists.getVersion());
						if (getRequests().size()> 0) {
							Stat s = zk.exists(requests+"/"+getRequests().get(0), false);
							byte[] op = zk.getData(requests+"/"+getRequests().get(0),false,s);
							zk.setData(cmd+command, op, exists2.getVersion());
							deleteNode(requests+"/"+getRequests().get(0));
						} else {
							noReq= true;
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Exception: wacherCnt");
			}
		}
	};
	
	//Watcher que notifica cuando se modifica el nodo /cmd/command (El comando a ejecutar)
	private Watcher  watcherCommand = new Watcher() {
		public void process(WatchedEvent event) {	
			try {
				try {
					Stat exists = zk.exists(cmd+command, watcherCommand);
					byte[] op = zk.getData(cmd+command,watcherCommand,exists);
					ByteArrayInputStream in = new ByteArrayInputStream(op);
					ObjectInputStream is = new ObjectInputStream(in);
					OperationBank operation = (OperationBank) is.readObject();
					Bank bankinter = Bank.getInstance();
					bankinter.handleReceiverMsg(operation);
					while(true) {
						byte[] counterono=ByteBuffer.allocate(4).putInt(0).array();
						Stat exists2 = zk.exists(cmd+cnt, watcherCnt);
						try {
							counterono=zk.getData(cmd+cnt, watcherCnt, exists2);
							int counterino = new BigInteger(counterono).intValue();
							counterino++;
							counterono=ByteBuffer.allocate(4).putInt(counterino).array();
							try {
								zk.setData(cmd+cnt, counterono, exists2.getVersion());
								break;
							}catch(Exception e){
								System.out.println("Couldn't set counter");
							}finally {
							}
						}catch(Exception e){
							System.out.println("Couldn't get counter");
						}finally {
						}				
					}
				} catch (KeeperException e) {
					System.out.println("The session with Zookeeper failes. Closing");
					return;
				} catch (InterruptedException e) {
					System.out.println("InterruptedException raised");
				}
			} catch (Exception e) {
				System.out.println("Exception: wacherCommand");
			}
		}
	};
	
	//Metodo que escribe la request en un nuevo nodo hijo de /req
	public void send(byte[] msg){
		try {
			Stat b = zk.exists(requests, false);
			zk.create(requests+ aRequest, msg, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		} catch (KeeperException e) {
			System.out.println("The session with Zookeeper fails. Closing");
			return;
		} catch (InterruptedException e) {
			System.out.println("InterruptedException raised");
		}
	}
	
	
	@Override
	public void process(WatchedEvent event) {
		
	}
	
	//Metodo que borra el nodo que se le indica
	public void deleteNode(String path) {
		try {
			Stat s = zk.exists(path, false);
			zk.delete(path, s.getVersion());
		}catch(Exception e){
			System.out.println("Couldn't delete node");
		}	
	}
	
	//Metodo que cierra la conexión de zk
	public void closeChannel () {
		try {
			zk.close();
		}catch(Exception e){
			System.out.println("Couldn't close connection");
		}	
	}
	
	//Metodo que envia la base de datos al nuevo miembro
	public void sendDB (String ip) throws InterruptedException {
		try {
			TimeUnit.SECONDS.sleep(5);
			Socket socket = new Socket(ip,7777);
			System.out.println("Sending database to new member with IP: "+ip);
			OutputStream os =socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(Bank.getInstance().getClientDB());
			socket.close();
			System.out.println("The database has been sent");
			System.out.println(">>> Enter opn cliente.: 1) Create. 2) Read. 3) Update. 4) Delete. 5) BankDB. 6) Exit");

		} catch (UnknownHostException e) {
			System.out.println("Couldn't send data");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Couldn't send data");
			e.printStackTrace();
		}
		
	}
	
	//Metodo que abre un socket en el puerto 7777 para recibir la BBDD al conectarse
	private void receiveDB() {
		System.out.println("Receiving DB...");
		Bank bank = Bank.getInstance();
		ServerSocket ss;
		Socket socket;
		try {
			ss = new ServerSocket(7777);
			socket = ss.accept();
			InputStream is = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			ClientDB db = (ClientDB) ois.readObject();
			bank.setClientDB(db);
			ss.close();
			socket.close();
			System.out.println("Received the DB!!");
		} catch (IOException e1) {
			System.out.println("Could not open connection to try and get the database");
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Could not retrieve the database");
			e.printStackTrace();
		}
	}
}