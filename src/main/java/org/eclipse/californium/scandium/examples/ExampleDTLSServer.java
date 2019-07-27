/*******************************************************************************
 * Copyright (c) 2015, 2017 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Stefan Jucker - DTLS implementation
 *    Bosch Software Innovations GmbH - migrate to SLF4J
 ******************************************************************************/
package org.eclipse.californium.scandium.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.concurrent.Executors;

import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

public class ExampleDTLSServer {

	private static final int DEFAULT_PORT = 20000;
	private static final Logger LOG = LoggerFactory
			.getLogger(ExampleDTLSServer.class.getName());
	private static final char[] KEY_STORE_PASSWORD = "endPass".toCharArray();
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";

	private DTLSConnector dtlsConnector;

	public ExampleDTLSServer(ExampleDTLSServerConfig config) {
		InMemoryPskStore pskStore = new InMemoryPskStore();
		// put in the PSK store the default identity/psk for tinydtls tests
		pskStore.setKey(config.getPskIdentity(), config.getPskKey());
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					new File("created").createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}));
		try {
			// load server credentials 
			SslContextUtil.Credentials serverCredentials = SslContextUtil.loadCredentials(
					SslContextUtil.CLASSPATH_SCHEME + KEY_STORE_LOCATION, "server", KEY_STORE_PASSWORD,
					KEY_STORE_PASSWORD);

			// load the trust store
			KeyStore trustStore = KeyStore.getInstance("JKS");
			InputStream inTrust = new FileInputStream(config.getTrustLocation()); 
			trustStore.load(inTrust, config.getTrustPassword().toCharArray());

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate(config.getTrustAlias());
			
			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
			builder.setPskStore(pskStore);
			builder.setAddress(new InetSocketAddress(DEFAULT_PORT));
			builder.setIdentity(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(),
					CertificateType.RAW_PUBLIC_KEY, CertificateType.X_509);
			builder.setSupportedCipherSuites(config.getCipherSuites().toArray(new CipherSuite [config.getCipherSuites().size()]));
			builder.setRetransmissionTimeout(config.getTimeout());
			builder.setTrustStore(trustedCertificates);
			builder.setClientAuthenticationRequired(config.isReqCert());
			builder.setEnableAddressReuse(true);
			dtlsConnector = new DTLSConnector(builder.build());
			dtlsConnector.setRawDataReceiver(new RawDataChannelImpl(dtlsConnector));

		} catch (GeneralSecurityException | IOException e) {
			LOG.error("Could not load the keystore", e);
		}

	}

	public void start() {
		try {
			dtlsConnector.start();
			System.out.println("DTLS example server started");
		} catch (IOException e) {
			throw new IllegalStateException(
					"Unexpected error starting the DTLS UDP server", e);
		}
	}
	
	public void stop() {
		if (dtlsConnector.isRunning()) {
			dtlsConnector.stop();
		}
	}


	private class RawDataChannelImpl implements RawDataChannel {

		private Connector connector;

		public RawDataChannelImpl(Connector con) {
			this.connector = con;
		}

		@Override
		public void receiveData(final RawData raw) {
			if (LOG.isInfoEnabled()) {
				LOG.info("Received request: {}", new String(raw.getBytes()));
			}
			RawData response = RawData.outbound("ACK".getBytes(),
					raw.getEndpointContext(), null, false);
			connector.send(response);
		}
	}

	public static void main(String[] args) {
		ExampleDTLSServerConfig config = new ExampleDTLSServerConfig();
		JCommander commander = new JCommander(config);
		commander.parse(args);
		if (config.isHelp()) {
			commander.usage();
			return;
		}
	
		ExampleDTLSServer server = new ExampleDTLSServer(config);
		server.start();
		try {
			for (;;) {
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			System.out.println(e);
			server.stop();
		}
	}
}
