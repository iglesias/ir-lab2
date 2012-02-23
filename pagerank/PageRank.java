/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version:  Johan Boye, 2012
 *   Student:        Fernando José Iglesias García, 2012
 *
 */  

import java.util.*;
import java.io.*;

public class PageRank{


    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    static boolean DEBUG = false;
        
    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a Hashtable, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a Hashtable whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The number of documents with no outlinks.
     */
    int numberOfSinks = 0;

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *  A random walk terminates with propbability 1-C
     */
    final static double C = 1 - BORED;  //TODO use the same as BORED?

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    /**
     *   Never do more than this number of iterations regardless
     *   of whether the transistion probabilities converge or not.
     */
    final static int MAX_NUMBER_OF_ITERATIONS = 1000;

    
    /* --------------------------------------------- */


    public PageRank( String filename ) {
	int noOfDocs = readDocs( filename );
	computePagerank( noOfDocs );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and creates the docs table. When this method 
     *   finishes executing then the @code{out} vector of outlinks is 
     *   initialised for each doc, and the @code{p} matrix is filled with
     *   zeroes (that indicate direct links) and NO_LINK (if there is no
     *   direct link. <p>
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previousy unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new Hashtable<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	    // Compute the number of sinks.
	    for ( int i=0; i<fileIndex; i++ ) {
		if ( out[i] == 0 )
		    numberOfSinks++;
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Computes the pagerank of each document.
     */
    void computePagerank( int N ) {


        // pi is the PageRank, the probability distribution
        final double[] pi = monteCarlo4( N, 1 );
        //final double[] pi = powerIteration( N );

	// Sort the pages by rank

	Integer[] idxs = new Integer[N];
	for ( int i = 0 ; i < N ; ++i ) idxs[i] = i;

	Arrays.sort(idxs, new Comparator<Integer>() {
		@Override public int compare(final Integer o1, final Integer o2) {
		    return -1 * Double.compare( pi[o1], pi[o2] );
		}
	});

	// Show the pages sorted
	for ( int i = 0 ; i < N ; ++i )
	    System.out.println(i+1 + ". " + docName[ idxs[i] ] + " " + pi[ idxs[i] ] );

        
        // --- DEBUG ---
        // Sum the elements of the PageRank
        if ( DEBUG ) {
            double sum = 0.0;
            for ( int i = 0 ; i < N ; ++i ) {
                sum += pi[i];
            }
            System.err.println(">>>> Sum(pi) = " + sum);
        }

    }

    private double[] powerIteration( int N ) {
    
	// PageRank vector
        double[] pi = new double[N];
	// Check for convergence criterion
	boolean stop = false;
	int iterations = 0;

	// Initial probabilities, assume the surfer is in a particular page
	pi[0] = 1.0;
	for ( int i = 1 ; i < N ; ++i )
	    pi[i] = 0.0;

	double[] piNext = new double[N];

	while ( iterations < MAX_NUMBER_OF_ITERATIONS && !stop ) {

	    for ( int i = 0 ; i < N ; ++i ) {

		// Initialize to the sum of the random jump for all the documents
		piNext[i] = BORED / N;
		for ( int j = 0 ; j < N ; ++j ) {
		    Hashtable<Integer, Boolean> outlinks = link.get(j);
		    if ( outlinks == null )
                      piNext[i] += pi[j] * (1-BORED) / N;
                    else if ( outlinks.get(i) != null && outlinks.get(i) )
                      piNext[i] += pi[j] * (1-BORED) / outlinks.size();
		}
	    }

	    // Finish to iterate?
	    stop = true;
	    for ( int i = 0 ; i < N ; ++i )
		if ( Math.abs( piNext[i] - pi[i] ) > EPSILON ) {
		    stop = false;
		    break;
		}

	    // Update the PageRank vector
	    for ( int i = 0 ; i < N ; ++i ) pi[i] = piNext[i];

	    ++iterations;

	}

        // --- DEBUG ---
        if ( DEBUG )
	    System.err.println(">>>> Power iteration finished after " 
                    + iterations + " iterations");

        return pi;

    }

    /**
     * Algorithm 1 described in Avrachenkov's et al. paper.
     * Simulate n runs of a random walk initiated at a randomly chose page. 
     * Evaluate the PageRank for each page as the fraction of random walks
     * which end at that page.
     *
     * @return the PageRank vector
     */
    private double[] monteCarlo1( int N, int nRandomWalks ) {
    
        // PageRank vector
        double[] pi = new double[N];
        // # random walks which end at every page
        int[] end = new int[N];
        for ( int i = 0 ; i < N ; ++i ) end[i] = 0;
        
        Random randomGen = new Random();

        // Simulate the random walks
        for ( int i = 0 ; i < nRandomWalks ; ++i ) {
            // Choose randomly an initial page
            int initPage = randomGen.nextInt(N);

            ++end[ randomWalk(N, initPage) ];
        }

        for (int i = 0 ; i < N ; ++i ) {
            pi[i] = end[i] / (double)nRandomWalks;
        }

        // --- DEBUG ---
        if ( DEBUG )
            System.err.println(">>>> Monte Carlo Algorithm 1 finished after "
                    + nRandomWalks + " random walks");

        return pi;

    }

    /**
     * Algorithm 4 described in Avrachenkov's et al. paper, complete path
     * stopping at dangling nodes.
     * Simulate random walks starting exactly m times from each page. Evaluate 
     * the PageRank for each page as the fraction of visits to that page divided
     * by the total number of visited pages.
     *
     * @return the PageRank vector
     */
    private double[] monteCarlo4( int N, int m ) {
    
        // PageRank vector
        double[] pi = new double[N];
        // # random visits to every page        
        int[] visits = new int[N];
        for ( int i = 0 ; i < N ; ++i ) visits[i] = 0;
        // Total number of visits
        int totalVisits = 0;
        
        // Simulate the random walks
        for ( int i = 0 ; i < m ; ++i )
            for ( int j = 0 ; j < N ; ++j )
                totalVisits += randomWalk(N, j, visits);

        for (int i = 0 ; i < N ; ++i )
            pi[i] = visits[i] / (double)totalVisits;

        // --- DEBUG ---
        if ( DEBUG )
            System.err.println(">>>> Monte Carlo Algorithm 4 finished after "
                    + m + " random walks from each page");

        return pi;

    }

    /**
     * Algorithm 5 described in Avrachenkov's et al. paper, complete path
     * with random start (stopping at dangling nodes also).
     * Simulate n random walks starting at a random page. Evaluate the PageRank 
     * for each page as the fraction of visits to that page divided by the total 
     * number of visited pages.
     *
     * @return the PageRank vector
     */
    private double[] monteCarlo5( int N, int nRandomWalks ) {
    
        // PageRank vector
        double[] pi = new double[N];
        // # random visits to every page        
        int[] visits = new int[N];
        for ( int i = 0 ; i < N ; ++i ) visits[i] = 0;
        // Total number of visits
        int totalVisits = 0;
      
        Random randomGen = new Random();

        // Simulate the random walks
        for ( int i = 0 ; i < nRandomWalks ; ++i ) {
            // Choose randomly an initial page
            int initPage = randomGen.nextInt(N);

            totalVisits += randomWalk(N, initPage, visits);
        }

        for (int i = 0 ; i < N ; ++i )
            pi[i] = visits[i] / (double)totalVisits;

        // --- DEBUG ---
        if ( DEBUG )
            System.err.println(">>>> Monte Carlo Algorithm 5 finished after "
                    + nRandomWalks + " random walks");

        return pi;

    }

    /**
     * Simulation of random walk 
     *
     * @return the page where the walk ends
     */
    private int randomWalk( int N, int actualPage ) {

        double random;
        boolean terminate = false;
        while ( ! terminate ) {
            
            // Generate a random number to test termination of the random walk
            random = Math.random();
            if ( random < (1 - C) ) {
                terminate = true;
            } else {
                // Generate a random number to do the transition to another page
                random = 1 - Math.random();  // random is in (0, 1]
                // Cumulative probability of the transitions already explored
                double cumsum = 0.0;
                for ( int i = 0 ; i < N ; ++i ) {

                    // Read the probability of going to page i from actualPage
                    double pij = 0.0;
                    Hashtable<Integer, Boolean> outlinks = link.get(actualPage);
                    if ( outlinks == null )
                        pij = 1 / (double)N;
                    else if ( outlinks.get(i) != null && outlinks.get(i) )
                        pij = 1 / (double)outlinks.size();

                    // Jump to i allowed if the transistion prob. is non-zero
                    if ( pij != 0.0 )
                        if ( random > cumsum && random <= cumsum + pij ) {
                            actualPage = i;
                            break;
                        }
                    
                    cumsum += pij;

                }
            }

        }

        return actualPage;

    }

    /**
     * Simulation of random walk 
     *
     * @return number of visited pages
     */
    private int randomWalk( int N, int actualPage, int[] visits ) {

        int totalVisits = 0;
        double random;
        boolean terminate = false;
        while ( ! terminate ) {
            
            // Update statistics
            ++totalVisits;
            ++visits[ actualPage ];

            // Generate a random number to test termination of the random walk
            random = Math.random();
            if ( random < (1 - C) ) {
                terminate = true;
            } else {
                // Generate a random number to do the transition to another page
                random = 1 - Math.random();  // random is in (0, 1]
                // Cumulative probability of the transitions already explored
                double cumsum = 0.0;
                for ( int i = 0 ; i < N ; ++i ) {

                    // Read the probability of going to page i from actualPage
                    double pij = 0.0;
                    Hashtable<Integer, Boolean> outlinks = link.get(actualPage);
                    if ( outlinks == null ) { 
                        // Dangling node (wihtout no outlinks)
                        terminate = true;
                        break;
                    } else if ( outlinks.get(i) != null && outlinks.get(i) )
                        pij = 1 / (double)outlinks.size();

                    // Jump to i allowed if the transistion prob. is non-zero
                    if ( pij != 0.0 )
                        if ( random > cumsum && random <= cumsum + pij ) {
                            actualPage = i;
                            break;
                        }
                    
                    cumsum += pij;

                }
            }

        }

        return totalVisits;

    }

    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length < 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
        else if ( args.length > 1 && args[1].equals("debug") ) {
            System.err.println(">>>> DEBUG mode activated");
            DEBUG = true;
            new PageRank( args[0] );
        }
	else {
	    new PageRank( args[0] );
	}
    }
}
