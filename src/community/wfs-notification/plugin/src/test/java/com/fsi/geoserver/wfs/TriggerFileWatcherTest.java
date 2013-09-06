package com.fsi.geoserver.wfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.forwardslope.c2rpc.triggers.Link;
import com.forwardslope.c2rpc.triggers.Trigger;

public class TriggerFileWatcherTest extends TestCase implements TestConstants {

    public void testLoadTriggers() throws IOException, SAXException {
        TriggerFileWatcher triggers = new TriggerFileWatcher(10000, WFSNotifyTest.class.getResource("triggers.xml"));
        Map<QName, List<Trigger>> map =
            triggers.load();
        assertTrue(map.containsKey(QN_TRACK));

        List<Trigger> list = map.get(QN_TRACK);
        assertEquals(1, list.size());

        Trigger t = list.get(0);
        assertTrue(t.isSilent());
        assertEquals(2, t.getLink().size());

        Link fl = t.getLink().get(0);
        assertEquals(0, fl.getTrigger().size());
        assertEquals("localKey", fl.getKey());
        assertEquals("localKey", fl.getForeign());
        assertEquals(QN_OTHERTYPE, fl.getDest());

        fl = t.getLink().get(1);
        assertEquals(1, fl.getTrigger().size());
        assertEquals("localKey2", fl.getKey());
        assertEquals("foreignKey", fl.getForeign());
        assertEquals(QN_THIRDTYPE, fl.getDest());

        Trigger t2 = fl.getTrigger().get(0);
        assertEquals(2, t2.getLink().size());
        assertFalse(t2.isSilent());

        fl = t2.getLink().get(0);
        assertEquals(0, fl.getTrigger().size());
        assertEquals("localKey", fl.getKey());
        assertEquals("localKey", fl.getForeign());
        assertEquals(QN_OTHERTYPE, fl.getDest());

        fl = t2.getLink().get(1);
        assertEquals(0, fl.getTrigger().size());
        assertEquals("localKey2", fl.getKey());
        assertEquals("foreignKey", fl.getForeign());
        assertEquals(QN_THIRDTYPE, fl.getDest());
    }

    private void copyTriggers(File toFile) throws IOException {
        InputStream is =
            WFSNotifyTest.class.getResource("triggers.xml").openStream();
        try {
            OutputStream os = new FileOutputStream(toFile);
            try {
                IOUtils.copy(is, os);
            } finally {
                IOUtils.closeQuietly(os);
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void testReload() throws Exception {
        final long CHECK_TIME = 1000;
        File f = File.createTempFile("com.fsi.geoserver.wfs.triggers", ".xml");
        copyTriggers(f);
        TriggerFileWatcher tfw = new TriggerFileWatcher(CHECK_TIME, f.toURI().toURL());
        Object o1 = tfw.load();
        assertNotNull(o1);

        // Check 1: modification before expiration time
        copyTriggers(f);
        Object o2 = tfw.load();
        assertSame(o1, o2);

        // Check 2: modification after expiration time
        Thread.sleep(CHECK_TIME * 2);
        copyTriggers(f);
        o2 = tfw.load();
        assertNotNull(o2);
        assertNotSame(o1, o2);
    }

}
