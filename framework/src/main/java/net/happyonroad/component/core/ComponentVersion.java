/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core;

import java.util.StringTokenizer;

/**
 * 组件版本，简化自 Maven ArtifactVersion
 */
public class ComponentVersion implements Comparable<ComponentVersion>{
    private Integer majorVersion;

    private Integer minorVersion;

    private Integer incrementalVersion;

    private Integer buildNumber;

    private String qualifier;

    private String unparsed;

    public ComponentVersion(String version)
    {
        parseVersion( version );
    }

    public int compareTo( ComponentVersion otherVersion )
    {

        int result = getMajorVersion() - otherVersion.getMajorVersion();
        if ( result == 0 )
        {
            result = getMinorVersion() - otherVersion.getMinorVersion();
        }
        if ( result == 0 )
        {
            result = getIncrementalVersion() - otherVersion.getIncrementalVersion();
        }
        if ( result == 0 )
        {
            if ( qualifier != null )
            {
                String otherQualifier = otherVersion.getQualifier();

                if ( otherQualifier != null )
                {
                    if ( ( qualifier.length() > otherQualifier.length() )
                         && qualifier.startsWith( otherQualifier ) )
                    {
                        // here, the longer one that otherwise match is considered older
                        result = -1;
                    }
                    else if ( ( qualifier.length() < otherQualifier.length() )
                              && otherQualifier.startsWith( qualifier ) )
                    {
                        // here, the longer one that otherwise match is considered older
                        result = 1;
                    }
                    else
                    {
                        result = qualifier.compareTo( otherQualifier );
                    }
                }
                else
                {
                    // otherVersion has no qualifier but we do - that's newer
                    result = -1;
                }
            }
            else if ( otherVersion.getQualifier() != null )
            {
                // otherVersion has a qualifier but we don't, we're newer
                result = 1;
            }
            else
            {
                result = getBuildNumber() - otherVersion.getBuildNumber();
            }
        }
        return result;
    }

    public int getMajorVersion()
    {
        return majorVersion != null ? majorVersion : 0;
    }

    public int getMinorVersion()
    {
        return minorVersion != null ? minorVersion : 0;
    }

    public int getIncrementalVersion()
    {
        return incrementalVersion != null ? incrementalVersion : 0;
    }

    public int getBuildNumber()
    {
        return buildNumber != null ? buildNumber : 0;
    }

    public String getQualifier()
    {
        return qualifier;
    }

    /**
     * 解析版本字符串
     *   1.0.1-1
     *   1.0.1-snapshot
     *
     * @param version 字符串
     */
    public final void parseVersion( String version )
    {
        unparsed = version;

        int index = version.indexOf( "-" );

        String part1;
        String part2 = null;

        if ( index < 0 )
        {
            part1 = version;
        }
        else
        {
            part1 = version.substring( 0, index );
            part2 = version.substring( index + 1 );
        }

        if ( part2 != null )
        {
            try
            {
                if ( ( part2.length() == 1 ) || !part2.startsWith( "0" ) )
                {
                    buildNumber = Integer.valueOf( part2 );
                }
                else
                {
                    qualifier = part2;
                }
            }
            catch ( NumberFormatException e )
            {
                qualifier = part2;
            }
        }

        if ( !part1.contains(".") && !part1.startsWith( "0" ) )
        {
            try
            {
                majorVersion = Integer.valueOf( part1 );
            }
            catch ( NumberFormatException e )
            {
                // qualifier is the whole version, including "-"
                qualifier = version;
                buildNumber = null;
            }
        }
        else
        {
            boolean fallback = false;

            StringTokenizer tok = new StringTokenizer( part1, "." );
            try
            {
                majorVersion = getNextIntegerToken( tok );
                if ( tok.hasMoreTokens() )
                {
                    minorVersion = getNextIntegerToken( tok );
                }
                if ( tok.hasMoreTokens() )
                {
                    incrementalVersion = getNextIntegerToken( tok );
                }
                if ( tok.hasMoreTokens() )
                {
                    fallback = true;
                }

                // string tokenzier won't detect these and ignores them
                if (part1.contains("..") || part1.startsWith( "." ) || part1.endsWith( "." ) )
                {
                    fallback = true;
                }
            }
            catch ( NumberFormatException e )
            {
                fallback = true;
            }

            if ( fallback )
            {
                // qualifier is the whole version, including "-"
                qualifier = version;
                majorVersion = null;
                minorVersion = null;
                incrementalVersion = null;
                buildNumber = null;
            }
        }
    }

    private static Integer getNextIntegerToken( StringTokenizer tok )
    {
        String s = tok.nextToken();
        if ( ( s.length() > 1 ) && s.startsWith( "0" ) )
        {
            throw new NumberFormatException( "Number part has a leading 0: '" + s + "'" );
        }
        return Integer.valueOf( s );
    }

    public String toString()
    {
        return unparsed;
    }

    public boolean equals( Object other )
    {
        //noinspection SimplifiableIfStatement
        if ( this == other )
        {
            return true;
        }

        return other instanceof ComponentVersion && 0 == compareTo((ComponentVersion) other);

    }

    public int hashCode()
    {
        int result = 1229;

        result = 1223 * result + getMajorVersion();
        result = 1223 * result + getMinorVersion();
        result = 1223 * result + getIncrementalVersion();
        result = 1223 * result + getBuildNumber();

        if ( null != getQualifier() )
        {
            result = 1223 * result + getQualifier().hashCode();
        }

        return result;
    }
}
