package accismus.api.mapreduce;

import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.junit.Assert;
import org.junit.Test;

import accismus.api.types.StringEncoder;
import accismus.api.types.TypeLayer;
import accismus.impl.Base;
import accismus.impl.TestTransaction;

public class MutationBuilderIT extends Base {

  static TypeLayer tl = new TypeLayer(new StringEncoder());

  @Test
  public void testBatchWrite() throws Exception {
    // test initializing an Accismus table by batch writing to it

    // use a batch writer to test this because its easier than using AccumuloOutputFormat
    BatchWriter bw = conn.createBatchWriter(table, new BatchWriterConfig());
    try {

      MutationBuilder mb1 = new MutationBuilder(new ArrayByteSequence("row1"));
      mb1.put(tl.newColumn("cf1", "cq1"), new ArrayByteSequence("v1"));
      mb1.put(tl.newColumn("cf1", "cq2"), new ArrayByteSequence("v2"));
      mb1.put(tl.newColumn("cf1", "cq3"), new ArrayByteSequence("v3"));

      bw.addMutation(mb1.build());

      MutationBuilder mb2 = new MutationBuilder(new ArrayByteSequence("row2"));
      mb2.put(tl.newColumn("cf1", "cq1"), new ArrayByteSequence("v4"));
      mb2.put(tl.newColumn("cf1", "cq2"), new ArrayByteSequence("v5"));

      bw.addMutation(mb2.build());

    } finally {
      bw.close();
    }

    TestTransaction tx1 = new TestTransaction(config);
    TestTransaction tx2 = new TestTransaction(config);

    Assert.assertEquals("v1", tx1.get().row("row1").fam("cf1").qual("cq1").toString());
    Assert.assertEquals("v2", tx1.get().row("row1").fam("cf1").qual("cq2").toString());
    Assert.assertEquals("v3", tx1.get().row("row1").fam("cf1").qual("cq3").toString());
    Assert.assertEquals("v4", tx1.get().row("row2").fam("cf1").qual("cq1").toString());
    Assert.assertEquals("v5", tx1.get().row("row2").fam("cf1").qual("cq2").toString());

    tx1.mutate().row("row1").fam("cf1").qual("cq2").set("v6");
    tx1.mutate().row("row1").fam("cf1").qual("cq3").delete();
    tx1.mutate().row("row2").fam("cf1").qual("cq2").set("v7");

    tx1.commit();

    // tx2 should see not changes from tx1
    Assert.assertEquals("v1", tx2.get().row("row1").fam("cf1").qual("cq1").toString());
    Assert.assertEquals("v2", tx2.get().row("row1").fam("cf1").qual("cq2").toString());
    Assert.assertEquals("v3", tx2.get().row("row1").fam("cf1").qual("cq3").toString());
    Assert.assertEquals("v4", tx2.get().row("row2").fam("cf1").qual("cq1").toString());
    Assert.assertEquals("v5", tx2.get().row("row2").fam("cf1").qual("cq2").toString());

    TestTransaction tx3 = new TestTransaction(config);

    // should see changes from tx1
    Assert.assertEquals("v1", tx3.get().row("row1").fam("cf1").qual("cq1").toString());
    Assert.assertEquals("v6", tx3.get().row("row1").fam("cf1").qual("cq2").toString());
    Assert.assertNull(tx3.get().row("row1").fam("cf1").qual("cq3").toString());
    Assert.assertEquals("v4", tx3.get().row("row2").fam("cf1").qual("cq1").toString());
    Assert.assertEquals("v7", tx3.get().row("row2").fam("cf1").qual("cq2").toString());
  }
}
