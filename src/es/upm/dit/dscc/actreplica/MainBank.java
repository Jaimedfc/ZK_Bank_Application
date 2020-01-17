package es.upm.dit.dscc.actreplica;

import java.util.Scanner;

public class MainBank {
	public MainBank() {
	}

	public void initMembers(Bank bank) {

		if (!bank.createClient(new Client(1, "Angel Alarcón", 100))) {
			return;
		}
		if (!bank.createClient(new Client(2, "Bernardo Bueno", 200))) {
			return;
		}
		if (!bank.createClient(new Client(3, "Carlos Cepeda", 300))) {
			return;
		}
		if (!bank.createClient(new Client(4, "Daniel Díaz", 400))) {
			return;
		}
		if (!bank.createClient(new Client(5, "Eugenio Escobar", 500))) {
			return;
		}
		if (!bank.createClient(new Client(6, "Fernando Ferrero", 600))) {
			return;
		}
	}
	
	public Client readClient(Scanner sc) {
		int accNumber = 0;
		String name   = null;
		int balance   = 0;
		
		System. out .print(">>> Enter account number (int) = ");
		if (sc.hasNextInt()) {
			accNumber = sc.nextInt();
		} else {
			System.out.println("The provised text provided is not an integer");
			sc.next();
			return null;
		}

		System. out .print(">>> Enter name (String) = ");
		name = sc.next();

		System. out .print(">>> Enter balance (int) = ");
		if (sc.hasNextInt()) {
			balance = sc.nextInt();
		} else {
			System.out.println("The provised text provided is not an integer");
			sc.next();
			return null;
		}
		return new Client(accNumber, name, balance);
	}

	public static void main(String[] args) {
		
		boolean correct = false;
		int     menuKey = 0;
		boolean exit    = false;
		Scanner sc      = new Scanner(System.in);
		int accNumber   = 0;
		int balance     = 0;
		Client client   = null;
		boolean status  = false;
		Bank bank = Bank.getInstance();
		MainBank mainBank = new MainBank();

		if (bank.isLeader()) {mainBank.initMembers(bank);}
		while (!exit) {
			try {
				correct = false;
				menuKey = 0;
				while (!correct) {
					System. out .println(">>> Enter opn cliente.: 1) Create. 2) Read. 3) Update. 4) Delete. 5) BankDB. 6) Exit");				
					if (sc.hasNextInt()) {
						menuKey = sc.nextInt();
						correct = true;
					} else {
						sc.next();
						System.out.println("The provised text provided is not an integer");
					}
				}

				switch (menuKey) {
				case 1: // Create client
					bank.createClient(mainBank.readClient(sc));
					break;
				case 2: // Read client
					System. out .print(">>> Enter account number (int) = ");
					if (sc.hasNextInt()) {
						accNumber = sc.nextInt();
						client = bank.readClient(accNumber);
						System.out.println(client);
					} else {
						System.out.println("The provised text provided is not an integer");
						sc.next();
					}
					break;
				case 3: // Update client
					System. out .print(">>> Enter account number (int) = ");
					if (sc.hasNextInt()) {
						accNumber = sc.nextInt();
					} else {
						System.out.println("The provised text provided is not an integer");
						sc.next();
					}
					System. out .print(">>> Enter balance (int) = ");
					if (sc.hasNextInt()) {
						balance = sc.nextInt();
					} else {
						System.out.println("The provised text provided is not an integer");
						sc.next();
					}
					bank.updateClient(accNumber, balance);
					break;
				case 4: // Delete client
					System. out .print(">>> Enter account number (int) = ");
					if (sc.hasNextInt()) {
						accNumber = sc.nextInt();
						status = bank.deleteClient(accNumber);
					} else {
						System.out.println("The provised text provided is not an integer");
						sc.next();
					}
					break;
				case 5:
					String aux = bank.toString();
					System.out.println(bank.toString());
					break;
				case 6:
					exit = true;	
					bank.close();
				default:
					break;
				}
			} catch (Exception e) {
				System.out.println("Exception at Main. Error read data");
			}

		}

		sc.close();
	}
}

