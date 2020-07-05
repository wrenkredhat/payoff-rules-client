package com.bmwfs.payoff.rules.rulesclient;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.kie.api.KieServices;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.ObjectFilter;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.CredentialsProvider;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.kie.server.client.credentials.EnteredCredentialsProvider;

import com.bmwfs.payoff.rules.model.Document;
import com.bmwfs.payoff.rules.model.Offer;


public class DocumentValidator {
	
	private static final String KIE_SERVER_URL = "http://localhost:8080/kie-server/services/rest/server";

	private static final String USERNAME = "kieserver";

	private static final String PASSWORD = "kieserver1!";

	private static final String STATELESS_KIE_SESSION_ID = "normal-stateless-ksession";

	// We use the container 'alias' instead of container name to decouple the client from the KIE-Contianer deployments.
	// private static final String CONTAINER_ID = "pomgr-doc-validation";
	private static final String CONTAINER_ID = "PayOffManager_1.0.0-SNAPSHOT";
	// private static final String CONTAINER_ID = "DocumentManagement";

	
	RuleServicesClient rulesClient;
	
	KieServices kieServices;
	
	protected void createServices() {
		
		kieServices = KieServices.Factory.get();

		CredentialsProvider credentialsProvider = new EnteredCredentialsProvider(USERNAME, PASSWORD);

		KieServicesConfiguration kieServicesConfig = KieServicesFactory.newRestConfiguration(KIE_SERVER_URL, credentialsProvider);

		// Set the Marshaling Format to JSON. Other options are JAXB and XSTREAM
		kieServicesConfig.setMarshallingFormat(MarshallingFormat.JSON);

		KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(kieServicesConfig);

		// Retrieve the RuleServices Client.
		rulesClient = kieServicesClient.getServicesClient(RuleServicesClient.class);
		
	}
	
	
	public Offer validateOffer(  Offer offer, Document... docs  ) {
	
		List<Command<?>> commands = new ArrayList<>();

		KieCommands commandFactory = kieServices.getCommands();
		// The identifiers that we provide in the insert commands can later be used to
		// retrieve the object from the response.
		commands.add(commandFactory.newInsert(offer, "offer"));
		
		commands.add(commandFactory.newInsertElements(  Arrays.asList(docs), "docs" ));
		commands.add(commandFactory.newFireAllRules());
				
		// commands.add( commandFactory.newGetObjects( new ClassObjectFilter(Document.class), "validatedDocuments" ));
		
		/*
		 * The BatchExecutionCommand contains all the commands we want to execute in the
		 * rules session, as well as the identifier of the session we want to use.
		 */
		BatchExecutionCommand batchExecutionCommand = commandFactory.newBatchExecution(commands, STATELESS_KIE_SESSION_ID );

		ServiceResponse<ExecutionResults> response = rulesClient.executeCommandsWithResults(CONTAINER_ID, batchExecutionCommand);

		ExecutionResults results = response.getResult();
		
		

		// specified in the Insert commands.
		Offer resultOffer  = (Offer) results.getValue("offer");
		
		// Collection<Document> valDocs = (Collection<Document>) results.getValue( "validatedDocuments" );
		
		Object valDocs = results.getValue("offer");
		
		System.out.println ("valDocs:" + valDocs );

		return resultOffer;
		
	}
	
	
	static public void main( String args[] )   {
		
		Offer o = new Offer();
		
		o.setChannel( "Digital"  );
		
		Document d = new Document();
		
		d.setDocumentId(new BigInteger("01"));
		d.setValid(false);
		
		d.setDocumentTypeId( new BigInteger("119") );
		
		DocumentValidator dv = new DocumentValidator();
		
		dv.createServices();
		dv.validateOffer(o, d);
		
		
	}
	
}
