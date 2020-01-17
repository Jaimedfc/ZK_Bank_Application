package es.upm.dit.dscc.actreplica;

import java.io.Serializable;

public class OperationBank implements Serializable {

	private static final long serialVersionUID = 1L;
	private OperationEnum operation;
	private Client        client        = null;
	private Integer       accountNumber = 0;
	private ClientDB      clientDB      = null;
	
	// When the node is going to be sent, the node must include the channel equal to null.
	// The relevant should be the address
	
   // ADD_CLIENT, UPDATE_CLIENT
	public OperationBank (OperationEnum operation,
			            Client client) {
		this.operation = operation;
		this.client    = client;
	}
	
	// READ_CLIENT, DELETE_CLIENT
	public OperationBank (OperationEnum operation,
						 Integer accountNumber ) {
		this.operation     = operation;
		this.accountNumber = accountNumber;
	}

	public OperationBank (OperationEnum operation) {
		this.operation = operation;
	}

	
	public OperationEnum getOperation() {
		return operation;
	}

	public void setOperation(OperationEnum operation) {
		this.operation = operation;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Integer getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(Integer accountNumber) {
		this.accountNumber = accountNumber;
	}
	
	public ClientDB getClientDB() {
		return clientDB;
	}

	public void setClientDB(ClientDB clientDB) {
		this.clientDB = clientDB;
	}
	
	@Override
	public String toString() {
		
		String string = null;
		
		string = "OperationBank [operation=" + operation;
		if (client != null) string = string + ", client=" + client.toString();
		string = string + ", accountNumber=" + accountNumber + "]\n";
		if (clientDB != null) string = string + clientDB.toString();
		
		return string;
	}
}
