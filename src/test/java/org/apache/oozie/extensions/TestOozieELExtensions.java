/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.extensions;

import java.io.File;

import org.apache.oozie.client.OozieClient;
import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.coord.SyncCoordAction;
import org.apache.oozie.coord.SyncCoordDataset;
import org.apache.oozie.coord.TimeUnit;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.service.ELService;
import org.apache.oozie.service.Services;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.ELEvaluator;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestOozieELExtensions {

    private ELEvaluator instEval;
    private ELEvaluator createEval;

    @BeforeClass
    public void setUp() throws Exception {
        String curPath = new File(".").getAbsolutePath();
        System.setProperty(Services.OOZIE_HOME_DIR, curPath);
        String confPath = new File(getClass().getResource("/oozie-site.xml").getFile()).getParent();
        System.setProperty(ConfigurationService.OOZIE_CONFIG_DIR, confPath);
        Services.setOozieHome();

        Services services = new Services();
        services.getConf().set("oozie.services", "org.apache.oozie.service.ELService");
        services.init();

        instEval = Services.get().get(ELService.class).createEvaluator("coord-action-create-inst");
        instEval.setVariable(OozieClient.USER_NAME, "test_user");
        instEval.setVariable(OozieClient.GROUP_NAME, "test_group");
        createEval = Services.get().get(ELService.class).createEvaluator("coord-action-create");
        createEval.setVariable(OozieClient.USER_NAME, "test_user");
        createEval.setVariable(OozieClient.GROUP_NAME, "test_group");
    }

    @Test
    public void testActionExpressions() throws Exception {
        ELEvaluator eval = createActionStartEvaluator();
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${now(0, 0)}"), "2009-09-02T10:00Z");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${now(1, 0)}"), "2009-09-02T11:00Z");
        
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${today(0, 0)}"), "2009-09-02T00:00Z");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${today(-1, 0)}"), "2009-09-01T23:00Z");

        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${yesterday(0, 0)}"), "2009-09-01T00:00Z");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${yesterday(0, 60)}"), "2009-09-01T01:00Z");
        
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${currentMonth(0, 0, 0)}"), "2009-09-01T00:00Z");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${currentMonth(-1, 0, 0)}"), "2009-08-31T00:00Z");
        
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${lastMonth(0, 0, 0)}"), "2009-08-01T00:00Z");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${lastMonth(0, 1, 0)}"), "2009-08-01T01:00Z");

        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${currentYear(0, 0, 0, 0)}"), "2009-01-01T00:00Z");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${currentYear(0, -1, 0, 0)}"), "2008-12-31T00:00Z");
        
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${lastYear(0, 0, 0, 0)}"), "2008-01-01T00:00Z");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${lastYear(1, 0, 0, 0)}"), "2008-02-01T00:00Z");
    }
    
    @Test
    public void testIntstanceTime() throws Exception {        
        ELEvaluator eval = createActionStartEvaluator();
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${instanceTime()}"), "2009-09-02T10:00Z");
    }
    
    @Test
    public void testDateOffset() throws Exception {
        ELEvaluator eval = createActionStartEvaluator();
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${dateOffset(instanceTime(), 1, 'DAY')}"), "2009-09-03T10:00Z");        
    }
    
    @Test
    public void testFormatTime() throws Exception {
        ELEvaluator eval = createActionStartEvaluator();
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${formatTime(instanceTime(), 'yyyy-MMM-dd')}"), "2009-Sep-02");                
    }
    
    @Test
    public void testUser() throws Exception {
        ELEvaluator eval = Services.get().get(ELService.class).createEvaluator("coord-action-start");
        eval.setVariable(OozieClient.USER_NAME, "test");
        Assert.assertEquals(CoordELFunctions.evalAndWrap(eval, "${user()}"), "test");                
        
    }
    
    @Test
    public void testDataIn() throws Exception {
        ELEvaluator eval = Services.get().get(ELService.class).createEvaluator("coord-action-start"); 
        String uris = "hdfs://localhost:8020/clicks/2010/01/01/00,hdfs://localhost:8020/clicks/2010/01/01/01";
        eval.setVariable(".datain.clicks", uris );
        String expuris = "hdfs://localhost:8020/clicks/2010/01/01/00/*/US,hdfs://localhost:8020/clicks/2010/01/01/01/*/US";
        Assert.assertEquals(expuris, CoordELFunctions.evalAndWrap(eval, "${dataIn('clicks', '*/US')}"));
    }
    
    @Test
    public void testCurrentMonth() throws Exception {
        initForCurrentThread();

        String expr = "${currentMonth(0,0,0)}";
        String instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-01T00:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));

        expr = "${currentMonth(2,-1,0)}";
        instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-02T23:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));
    }

    private String getELExpression(String expr) {
        if(expr != null) {
            return "${" + expr + "}";
        }
        return null;
    }

    @Test
    public void testToday() throws Exception {
        initForCurrentThread();

        String expr = "${today(0,0)}";
        String instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-02T00:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));

        expr = "${today(1,-20)}";
        instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-02T00:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));
    }

    @Test
    public void testNow() throws Exception {
        initForCurrentThread();

        String expr = "${now(0,0)}";
        String instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-02T10:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));

        expr = "${now(2,-10)}";
        instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-02T12:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));
    }

    @Test
    public void testYesterday() throws Exception {
        initForCurrentThread();

        String expr = "${yesterday(0,0)}";
        String instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-01T00:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));

        expr = "${yesterday(1,10)}";
        instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-09-01T01:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));
    }

    @Test
    public void testLastMonth() throws Exception {
        initForCurrentThread();

        String expr = "${lastMonth(0,0,0)}";
        String instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-08-01T00:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));

        expr = "${lastMonth(1,1,10)}";
        instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-08-02T01:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));
    }

    @Test
    public void testCurrentYear() throws Exception {
        initForCurrentThread();

        String expr = "${currentYear(0,0,0,0)}";
        String instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-01-01T00:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));

        expr = "${currentYear(1,0,1,0)}";
        instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2009-02-01T01:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));
    }

    @Test
    public void testLastYear() throws Exception {
        initForCurrentThread();

        String expr = "${lastYear(0,0,0,0)}";
        String instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2008-01-01T00:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));

        expr = "${lastYear(1,0,1,0)}";
        instResult = CoordELFunctions.evalAndWrap(instEval, expr);
        Assert.assertEquals("2008-02-01T01:00Z", CoordELFunctions.evalAndWrap(createEval, getELExpression(instResult)));
    }

    @Test
    public void testNominalGreaterThanInitial() throws Exception {
        initForCurrentThread("2009-08-30T010:00Z", "2009-09-02T11:30Z", "2009-09-02T10:30Z");
        String expr = "${currentYear(0,0,0,0)}";
        Assert.assertEquals("", CoordELFunctions.evalAndWrap(instEval, expr));
    }

    private void initForCurrentThread() throws Exception {
        initForCurrentThread("2007-09-30T010:00Z", "2009-09-02T11:30Z", "2009-09-02T10:30Z");
    }

    private SyncCoordDataset createDataSet(String initialInstance) throws Exception {
        SyncCoordDataset ds;
        ds = new SyncCoordDataset();
        ds.setFrequency(1);
        ds.setInitInstance(DateUtils.parseDateUTC(initialInstance));
        ds.setTimeUnit(TimeUnit.HOUR);
        ds.setTimeZone(DateUtils.getTimeZone("UTC"));
        ds.setName("test");
        ds.setUriTemplate("hdfs://localhost:9000/user/test_user/US/${YEAR}/${MONTH}/${DAY}");
        ds.setType("SYNC");
        ds.setDoneFlag("");
        return ds;
    }
    
    private SyncCoordAction createCoordAction(String actualTime, String nominalTime) throws Exception {
        SyncCoordAction appInst;
        appInst = new SyncCoordAction();
        appInst.setActualTime(DateUtils.parseDateUTC(actualTime));
        appInst.setNominalTime(DateUtils.parseDateUTC(nominalTime));
        appInst.setTimeZone(DateUtils.getTimeZone("UTC"));
        appInst.setActionId("00000-oozie-C@1");
        appInst.setName("mycoordinator-app");
        return appInst;
    }
    
    private void initForCurrentThread(String initialInstance, String actualTime, String nominalTime) throws Exception {
        SyncCoordDataset ds = createDataSet(initialInstance);
        SyncCoordAction appInst = createCoordAction(actualTime, nominalTime);
        CoordELFunctions.configureEvaluator(instEval, ds, appInst);
        CoordELFunctions.configureEvaluator(createEval, ds, appInst);
    }
    
    private ELEvaluator createActionStartEvaluator() throws Exception {
        SyncCoordAction appInst = createCoordAction("2009-09-02T11:30Z", "2009-09-02T10:00Z");
        ELEvaluator eval = Services.get().get(ELService.class).createEvaluator("coord-action-start");
        CoordELFunctions.configureEvaluator(eval, null, appInst);
        return eval;
    }
}
