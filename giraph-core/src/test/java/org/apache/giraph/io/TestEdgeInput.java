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

package org.apache.giraph.io;

import org.apache.giraph.BspCase;
import org.apache.giraph.conf.GiraphClasses;
import org.apache.giraph.vertex.EdgeListVertex;
import org.apache.giraph.io.formats.IdWithValueTextOutputFormat;
import org.apache.giraph.io.formats.IntIntTextVertexValueInputFormat;
import org.apache.giraph.io.formats.IntNullTextEdgeInputFormat;
import org.apache.giraph.utils.InternalVertexRunner;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * A test case to ensure that loading a graph from a list of edges works as
 * expected.
 */
public class TestEdgeInput extends BspCase {
  public TestEdgeInput() {
    super(TestEdgeInput.class.getName());
  }

  // It should be able to build a graph starting from the edges only.
  // Vertices should be implicitly created with default values.
  @Test
  public void testEdgesOnly() throws Exception {
    String[] edges = new String[] {
        "1 2",
        "2 3",
        "2 4",
        "4 1"
    };

    GiraphClasses classes = new GiraphClasses();
    classes.setVertexClass(TestVertexWithNumEdges.class);
    classes.setEdgeInputFormatClass(IntNullTextEdgeInputFormat.class);
    classes.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Map<String, String> params = ImmutableMap.of();
    Iterable<String> results = InternalVertexRunner.run(classes, params,
        null, edges);

    Map<Integer, Integer> values = parseResults(results);

    // Check that all vertices with outgoing edges have been created
    assertEquals(3, values.size());
    // Check the number of edges for each vertex
    assertEquals(1, (int) values.get(1));
    assertEquals(2, (int) values.get(2));
    assertEquals(1, (int) values.get(4));
  }

  // It should be able to build a graph by specifying vertex data and edges
  // as separate input formats.
  @Test
  public void testMixedFormat() throws Exception {
    String[] vertices = new String[] {
        "1 75",
        "2 34",
        "3 13",
        "4 32"
    };
    String[] edges = new String[] {
        "1 2",
        "2 3",
        "2 4",
        "4 1",
        "5 3"
    };

    GiraphClasses classes = new GiraphClasses();
    classes.setVertexClass(TestVertexDoNothing.class);
    classes.setVertexInputFormatClass(IntIntTextVertexValueInputFormat.class);
    classes.setEdgeInputFormatClass(IntNullTextEdgeInputFormat.class);
    classes.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Map<String, String> emptyParams = ImmutableMap.of();

    // Run a job with a vertex that does nothing
    Iterable<String> results = InternalVertexRunner.run(classes, emptyParams,
        vertices, edges);

    Map<Integer, Integer> values = parseResults(results);

    // Check that all vertices with either initial values or outgoing edges
    // have been created
    assertEquals(5, values.size());
    // Check that the vertices have been created with correct values
    assertEquals(75, (int) values.get(1));
    assertEquals(34, (int) values.get(2));
    assertEquals(13, (int) values.get(3));
    assertEquals(32, (int) values.get(4));
    // A vertex with edges but no initial value should have the default value
    assertEquals(0, (int) values.get(5));

    classes = new GiraphClasses();
    classes.setVertexClass(TestVertexWithNumEdges.class);
    classes.setVertexInputFormatClass(IntIntTextVertexValueInputFormat.class);
    classes.setEdgeInputFormatClass(IntNullTextEdgeInputFormat.class);
    classes.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);

    // Run a job with a vertex that counts outgoing edges
    results = InternalVertexRunner.run(classes, emptyParams, vertices, edges);

    values = parseResults(results);

    // Check the number of edges for each vertex
    assertEquals(1, (int) values.get(1));
    assertEquals(2, (int) values.get(2));
    assertEquals(0, (int) values.get(3));
    assertEquals(1, (int) values.get(4));
    assertEquals(1, (int) values.get(5));
  }

  public static class TestVertexWithNumEdges extends EdgeListVertex<IntWritable,
      IntWritable, NullWritable, NullWritable> {
    @Override
    public void compute(Iterable<NullWritable> messages) throws IOException {
      setValue(new IntWritable(getNumEdges()));
      voteToHalt();
    }
  }

  public static class TestVertexDoNothing extends EdgeListVertex<IntWritable,
      IntWritable, NullWritable, NullWritable> {
    @Override
    public void compute(Iterable<NullWritable> messages) throws IOException {
      voteToHalt();
    }
  }

  private static Map<Integer, Integer> parseResults(Iterable<String> results) {
    Map<Integer, Integer> values = Maps.newHashMap();
    for (String line : results) {
      String[] tokens = line.split("\\s+");
      int id = Integer.valueOf(tokens[0]);
      int value = Integer.valueOf(tokens[1]);
      values.put(id, value);
    }
    return values;
  }
}
