package com.socialislands.viz;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.Math;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.generator.plugin.RandomGraph;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerFactory;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.*;
import org.gephi.ranking.api.*;
import org.gephi.ranking.plugin.transformer.*;
import org.gephi.partition.api.*;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.statistics.plugin.*;

import org.openide.util.Lookup;

import org.gephi.io.exporter.spi.CharacterExporter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.gephi.filters.plugin.graph.KCoreBuilder;

/**
 *
 * @author weidongyang
 */

public class ScoringApp extends App
{

    public void run() {

        //Init a project - and therefore a workspace
        System.out.println("Run started...");
        try {
            mongoDB2Graph();
//            add2Graph("4f63c6e23f033175fe000004");
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        
        System.out.println("From formed graph, Nodes: "+undirectedGraph.getNodeCount()+" Edges: "+undirectedGraph.getEdgeCount());

        System.out.println("Start gen graph...");
        generateResult();
        
        System.out.println("Start export graph...");
        exportToMongo();
        System.out.println("Done...");
    }
    
    /**
     * 
     * @param s
     * @throws UnknownHostException
     */
    public ScoringApp(final String s) throws UnknownHostException {
        super(s);
    }

    
}
