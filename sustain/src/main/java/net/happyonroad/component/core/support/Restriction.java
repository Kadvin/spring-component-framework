/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.ComponentVersion;

/**
 * 限制，从Maven Restriction copy过来
 */
public class Restriction
{
    private final ComponentVersion lowerBound;

    private final boolean lowerBoundInclusive;

    private final ComponentVersion upperBound;

    private final boolean upperBoundInclusive;

    static final Restriction EVERYTHING = new Restriction(null, false, null, false);

    public Restriction(ComponentVersion lowerBound, boolean lowerBoundInclusive, ComponentVersion upperBound,
                       boolean upperBoundInclusive) {
        this.lowerBound = lowerBound;
        this.lowerBoundInclusive = lowerBoundInclusive;
        this.upperBound = upperBound;
        this.upperBoundInclusive = upperBoundInclusive;
    }

    public ComponentVersion getLowerBound()
    {
        return lowerBound;
    }

    public boolean isLowerBoundInclusive()
    {
        return lowerBoundInclusive;
    }

    public ComponentVersion getUpperBound()
    {
        return upperBound;
    }

    public boolean isUpperBoundInclusive()
    {
        return upperBoundInclusive;
    }

    public boolean containsVersion( ComponentVersion version )
    {
        if ( lowerBound != null )
        {
            int comparison = lowerBound.compareTo( version );
            if ( comparison == 0 && !lowerBoundInclusive )
            {
                return false;
            }
            if ( comparison > 0 )
            {
                return false;
            }
        }
        if ( upperBound != null )
        {
            int comparison = upperBound.compareTo( version );
            if ( comparison == 0 && !upperBoundInclusive )
            {
                return false;
            }
            if ( comparison < 0 )
            {
                return false;
            }
        }
        return true;
    }


    public int hashCode()
    {
        int result = 13;

        if ( lowerBound == null )
        {
            result += 1;
        }
        else
        {
            result += lowerBound.hashCode();
        }

        result *= lowerBoundInclusive ? 1 : 2;

        if ( upperBound == null )
        {
            result -= 3;
        }
        else
        {
            result -= upperBound.hashCode();
        }

        result *= upperBoundInclusive ? 2 : 3;

        return result;
    }

    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !(other instanceof Restriction ) )
        {
            return false;
        }

        Restriction restriction = (Restriction) other;
        if ( lowerBound != null )
        {
            if ( !lowerBound.equals( restriction.lowerBound ) )
            {
                return false;
            }
        }
        else if ( restriction.lowerBound != null )
        {
            return false;
        }

        if ( lowerBoundInclusive != restriction.lowerBoundInclusive )
        {
            return false;
        }

        if ( upperBound != null )
        {
            if ( !upperBound.equals( restriction.upperBound ) )
            {
                return false;
            }
        }
        else if ( restriction.upperBound != null )
        {
            return false;
        }

        return upperBoundInclusive == restriction.upperBoundInclusive;

    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();

        buf.append( isLowerBoundInclusive() ? "[" : "(" );
        if ( getLowerBound() != null )
        {
            buf.append( getLowerBound().toString() );
        }
        buf.append( "," );
        if ( getUpperBound() != null )
        {
            buf.append( getUpperBound().toString() );
        }
        buf.append( isUpperBoundInclusive() ? "]" : ")" );

        return buf.toString();
    }
}
