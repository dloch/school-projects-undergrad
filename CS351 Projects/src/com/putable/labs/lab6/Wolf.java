package com.putable.labs.lab6;

import com.putable.labs.lab6.abstracts.AbstractOmnivore;

/**
 * This is a concrete {@code Wolf} class. An instance of this class is used
 * specifically to be compared to other living things by fact of who would win
 * in a fight. A fight outcome is based on the attributes of a {@code Wolf}
 * including weight and what they consume for food (i.e. meat, plants or both).
 * 
 * @author BKey
 * 
 */
public class Wolf extends AbstractOmnivore
{
    private String kingdom = "Canis";
    
    /**
     * The constructor of a {@code Wolf}.
     * 
     * @param weight
     *            the {@code double} value that is the weight in lbs of the
     *            {@code Wolf}
     */
    public Wolf(double weight)
    {
	super(weight);
    }

    @Override
    public String getKingdom()
    {
	return kingdom;
    }

    @Override
    public String toString()
    {
	return "Wolf";
    }
}
