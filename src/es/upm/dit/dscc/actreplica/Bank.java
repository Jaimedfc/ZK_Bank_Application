package es.upm.dit.dscc.actreplica;

import java.util.List;

public class Bank {

	private static zGroup channel = null;
	private ClientDB clientDB = new ClientDB();
	private static SendMessagesBank sendMessages =null;
	public static Bank bank = null;
	
	public Bank() {};
	public static Bank getInstance(){
		if (bank == null) {
			bank = new Bank();
			try {
				channel = new zGroup();
			} catch (Exception e) {
				System.out.println("Could not create the zGroup");
			}
			sendMessages = new SendMessagesBank(channel);
		}
		return bank;
	}

	
	public boolean isLeader() {
		return channel.getIsLeader();
	}
	
	public void close() {
		channel.closeChannel();
		System.exit(0);
	}
	
	public void handleReceiverMsg(OperationBank operation) {
		switch (operation.getOperation()) {
		case CREATE_CLIENT:
			clientDB.createClient(operation.getClient());
			break;
		case READ_CLIENT:
			clientDB.readClient(operation.getAccountNumber());
			break;
		case UPDATE_CLIENT:
			clientDB.updateClient(operation.getClient().getAccountNumber(), 
					              operation.getClient().getBalance());
			break;
		case DELETE_CLIENT:
			clientDB.deleteClient(operation.getAccountNumber());
			break;
		case CREATE_BANK:
			System.out.println("Received CREATE_BANK");
			clientDB.createBank(operation.getClientDB());
			break;
		}
	}

	public boolean createClient(Client client) {	
		sendMessages.sendAdd(client);
		return true;
	}

	public Client readClient(Integer accountNumber) {
		return clientDB.readClient(accountNumber);
		
	}

	public boolean updateClient (int accNumber, int balance) {
		Client updatedClient = clientDB.readClient(accNumber);
		updatedClient.setBalance(balance);
		sendMessages.sendUpdate(updatedClient);
		return true;
	}

	public boolean deleteClient(Integer accountNumber) {
		sendMessages.sendDelete(accountNumber);
		return true;
	}

	public String toString() {
		String string = null;
		string = "          Bank Java     \n" +
				"------------------------\n";
		string = clientDB.toString();
		return string;
	}
	
	public ClientDB getClientDB() {
		return this.clientDB;
	}
	
	public void setClientDB(ClientDB db) {
		this.clientDB = db;
	}
	
}
