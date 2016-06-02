/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.examples.broker;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.conf.MBeansOption;
import org.drools.examples.broker.events.Event;
import org.drools.examples.broker.events.EventReceiver;
import org.drools.examples.broker.model.Company;
import org.drools.examples.broker.model.CompanyRegistry;
import org.drools.examples.broker.model.StockTick;
import org.drools.examples.broker.ui.BrokerWindow;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.api.runtime.rule.EntryPoint;

/**
 * The broker application
 *  
 * @author etirelli
 */
public class Broker implements EventReceiver, BrokerServices {
    private static final String[] ASSET_FILES = { "/broker.drl", "/notify.drl", "/position.rf", "/position.drl" };
    
    private BrokerWindow window;
    private CompanyRegistry companies;
   // private StatefulKnowledgeSession session;
    private KieSession session;
  
   // private WorkingMemoryEntryPoint tickStream;
    private EntryPoint tickStream;
    

    public Broker(BrokerWindow window,
                  CompanyRegistry companies) {
        super();
        this.window = window;
        this.companies = companies;
        //this.session = createSession();
        this.session = createSession();
        //this.tickStream = this.session.getWorkingMemoryEntryPoint( "StockTick stream" );
        this.tickStream =  this.session.getEntryPoint("StockTick stream");
    }

    @SuppressWarnings("unchecked")
    public void receive(Event<?> event) {
        try {
            StockTick tick = ((Event<StockTick>) event).getObject();
            Company company = this.companies.getCompany( tick.getSymbol() );
            this.tickStream.insert( tick );
            this.session.getAgenda().getAgendaGroup( "evaluation" ).setFocus();
            this.session.fireAllRules();
           
            window.updateCompany( company.getSymbol() );
            window.updateTick( tick );
            
        } catch ( Exception e ) {
            System.err.println("=============================================================");
            System.err.println("Unexpected exception caught: "+e.getMessage() );
            e.printStackTrace();
        }
    }
    
   // private StatefulKnowledgeSession createSession() {
    private KieSession createSession() {
    
       // KnowledgeBase kbase = loadRuleBase();
        //StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession();
    	
        //KnowledgeRuntimeLogger logger = KnowledgeRuntimeLoggerFactory.newConsoleLogger( session );
       KieSession session = createKbase();
        session.setGlobal( "services", this );
        for( Company company : this.companies.getCompanies() ) {
            session.insert( company );
        }
        session.fireAllRules();
        return session;
    }

    private KnowledgeBase loadRuleBase() {
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        try {
            for( int i = 0; i < ASSET_FILES.length; i++ ) {
                builder.add( ResourceFactory.newInputStreamResource( Broker.class.getResourceAsStream( ASSET_FILES[i] ) ),
                             ResourceType.determineResourceType( ASSET_FILES[i] ));
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 0 );
        }
        if( builder.hasErrors() ) {
            System.err.println(builder.getErrors());
            System.exit( 0 );
        }
        KnowledgeBaseConfiguration conf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        conf.setOption( EventProcessingOption.STREAM );
        conf.setOption( MBeansOption.ENABLED );
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase( "Stock Broker", conf ); 
        kbase.addKnowledgePackages( builder.getKnowledgePackages() );
        return kbase;
    }

    public KieSession createKbase(){
    	KieServices ks = KieServices.Factory.get();
        KieContainer kContainer = ks.getKieClasspathContainer();
        
        KieBaseConfiguration config = KieServices.Factory.get().newKieBaseConfiguration();
       config.setOption( org.kie.api.conf.EventProcessingOption.STREAM );
       
        KieBase kBase = kContainer.newKieBase(config);
        KieSessionConfiguration ksConfig = KieServices.Factory.get().newKieSessionConfiguration();
               
        KieSession kSession = kBase.newKieSession(ksConfig, null);
        kSession.addEventListener( new DebugRuleRuntimeEventListener() );
       //use less api
        //KieRuntimeLogger krLogger = ks.getLoggers().newConsoleLogger(kSession);
        //KieRuntimeLogger krLogger = ks.getLoggers().newFileLogger(kSession,"/opt/development/test.log");
       // EntryPoint entrypoint = kSession.getEntryPoint("StockTick stream");
        
        return kSession;
        
    }
    public void log(String message) {
        window.log( message );
    }
}
