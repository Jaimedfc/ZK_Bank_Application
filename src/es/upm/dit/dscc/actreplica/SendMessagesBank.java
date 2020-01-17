package es.upm.dit.dscc.actreplica;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class SendMessagesBank implements SendMessages {

	private zGroup channel;
	
	public SendMessagesBank(zGroup channel)  {
		this.channel = channel;
	}
	
	private void sendMessage( OperationBank operation) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(out);
			os.writeObject(operation);
			byte[] msg = out.toByteArray();
			channel.send(msg);
		} catch (Exception e) {
			System.err.println(e);
			System.out.println("Error when sending message");
			e.printStackTrace();
		}
	}

	public void sendAdd(Client client) {
		OperationBank operation = new OperationBank(OperationEnum.CREATE_CLIENT, client);
		sendMessage(operation);
	}

	public void sendRead(Integer accountNumber) {
		OperationBank operation = new OperationBank(OperationEnum.READ_CLIENT, accountNumber);
		sendMessage(operation);
	}

	public void sendUpdate(Client client) {
		OperationBank operation = new OperationBank(OperationEnum.UPDATE_CLIENT, client);
		sendMessage(operation);
	}

	public void sendDelete(Integer accountNumber) {
		OperationBank operation = new OperationBank(OperationEnum.DELETE_CLIENT, accountNumber);
		sendMessage(operation);
	}
	
	public void sendCreateBank () {
		OperationBank operation = new OperationBank(OperationEnum.CREATE_BANK);
		sendMessage(operation);
	}

}
