/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
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
 ******************************************************************************/
package org.eclipse.californium.scandium.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;

import com.beust.jcommander.JCommander;

import eu.javaspecialists.tjsn.concurrency.stripedexecutor.StripedExecutorService;

public class ExampleDTLSServer {

	static {
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINEST);
	}

	private static final Logger LOG = Logger.getLogger(ExampleDTLSServer.class.getName());
	private static final String KEY_STORE_PASSWORD = "endPass";
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";

	private DTLSConnector dtlsConnector;
	
	public ExampleDTLSServer(ExampleDTLSServerConfig config) {
		InMemoryPskStore pskStore = new InMemoryPskStore();
		// put in the PSK store the default identity/psk for tinydtls tests
		pskStore.setKey(config.getPskIdentity(), config.getPskKey());
		InputStream in = null;
		try {
			// load the key store
			KeyStore keyStore = KeyStore.getInstance("JKS");
			in = getClass().getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());
			in.close();

			// load the trust store
			KeyStore trustStore = KeyStore.getInstance("JKS");
			InputStream inTrust = new FileInputStream(config.getTrustLocation()); 
			trustStore.load(inTrust, config.getTrustPassword().toCharArray());

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate(config.getTrustAlias());

			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(config.getPort()));
			builder.setPskStore(pskStore);
			builder.setIdentity((PrivateKey)keyStore.getKey("server", KEY_STORE_PASSWORD.toCharArray()),
					keyStore.getCertificateChain("server"), true);
			builder.setSupportedCipherSuites(config.getCipherSuites().toArray(new CipherSuite [config.getCipherSuites().size()]));
			builder.setRetransmissionTimeout(config.getTimeout());
			builder.setTrustStore(trustedCertificates);
			builder.setClientAuthenticationRequired(config.isReqCert());
			builder.setEnableAddressReuse(true);
			dtlsConnector = new DTLSConnector(builder.build());
			dtlsConnector.setRawDataReceiver(new RawDataChannelImpl(dtlsConnector));

		} catch (GeneralSecurityException | IOException e) {
			LOG.log(Level.SEVERE, "Could not load the keystore", e);
		}
		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Cannot close key store file", e);
				}
			}
		}

	}
	
//	use this if you really want to make it use the single threaded executor
	private StripedExecutorService newSingleThreadedStripedExecutorService() {
		try {
			Constructor<StripedExecutorService> constr = StripedExecutorService.class.getDeclaredConstructor(ExecutorService.class);
			constr.setAccessible(true);
			StripedExecutorService stripedService = constr.newInstance(Executors.newSingleThreadExecutor());
			return stripedService;
		} catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | InstantiationException re) {
			LOG.log(Level.SEVERE, "Reflection exception", re);
			throw new RuntimeException(re);
		}
	}
	
	public void start() {
		try {
			dtlsConnector.start();
		} catch (IOException e) {
			throw new IllegalStateException("Unexpected error starting the DTLS UDP server",e);
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
			LOG.log(Level.INFO, "Received request: {0}", new String(raw.getBytes()));
			connector.send(new RawData("ACK".getBytes(), raw.getAddress(), raw.getPort()));
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
