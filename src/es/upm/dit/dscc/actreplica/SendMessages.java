package es.upm.dit.dscc.actreplica;

public interface SendMessages {

	public void sendAdd(Client client);

	public void sendRead(Integer accountNumber);

	public void sendUpdate(Client client);

	public void sendDelete(Integer accountNumber);
	
	public void sendCreateBank ();

}
