/*
 * Copyright 2019-2024 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.util;


import com.google.common.base.Preconditions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import org.polypheny.db.util.temporal.DateTimeUtils;


/**
 * Timestamp with time-zone literal.
 * <p>
 * Immutable, internally represented as a string (in ISO format), and can support unlimited precision (milliseconds, nanoseconds).
 */
public class TimestampWithTimeZoneString implements Comparable<TimestampWithTimeZoneString> {

    final TimestampString localDateTime;
    final TimeZone timeZone;
    final String v;


    /**
     * Creates a TimestampWithTimeZoneString.
     */
    public TimestampWithTimeZoneString( TimestampString localDateTime, TimeZone timeZone ) {
        this.localDateTime = localDateTime;
        this.timeZone = timeZone;
        this.v = localDateTime.toString() + " " + timeZone.getID();
    }


    /**
     * Creates a TimestampWithTimeZoneString.
     */
    public TimestampWithTimeZoneString( String v ) {
        this.localDateTime = new TimestampString( v.substring( 0, v.indexOf( ' ', 11 ) ) );
        String timeZoneString = v.substring( v.indexOf( ' ', 11 ) + 1 );
        Preconditions.checkArgument( DateTimeStringUtils.isValidTimeZone( timeZoneString ) );
        this.timeZone = TimeZone.getTimeZone( timeZoneString );
        this.v = v;
    }


    /**
     * Creates a TimestampWithTimeZoneString for year, month, day, hour, minute, second, millisecond values in the given time-zone.
     */
    public TimestampWithTimeZoneString( int year, int month, int day, int h, int m, int s, String timeZone ) {
        this( DateTimeStringUtils.ymdhms( new StringBuilder(), year, month, day, h, m, s ) + " " + timeZone );
    }


    /**
     * Sets the fraction field of a {@code TimestampWithTimeZoneString} to a given number of milliseconds.
     * <p>
     * For example, {@code new TimestampWithTimeZoneString(1970, 1, 1, 2, 3, 4, "GMT").withMillis(56)} yields {@code TIMESTAMP WITH LOCAL TIME ZONE '1970-01-01 02:03:04.056 GMT'}.
     */
    public TimestampWithTimeZoneString withMillis( int millis ) {
        Preconditions.checkArgument( millis >= 0 && millis < 1000 );
        return withFraction( DateTimeStringUtils.pad( 3, millis ) );
    }


    /**
     * Sets the fraction field of a {@code TimestampString}. The precision is determined by the number of leading zeros.
     * Trailing zeros are stripped.
     * <p>
     * For example, {@code new TimestampWithTimeZoneString(1970, 1, 1, 2, 3, 4, "GMT").withFraction("00506000")}
     * yields {@code TIMESTAMP WITH LOCAL TIME ZONE '1970-01-01 02:03:04.00506 GMT'}.
     */
    public TimestampWithTimeZoneString withFraction( String fraction ) {
        return new TimestampWithTimeZoneString( localDateTime.withFraction( fraction ), timeZone );
    }


    public TimestampWithTimeZoneString withTimeZone( TimeZone timeZone ) {
        if ( this.timeZone.equals( timeZone ) ) {
            return this;
        }
        String localDateTimeString = localDateTime.toString();
        String v;
        String fraction;
        int i = localDateTimeString.indexOf( '.' );
        if ( i >= 0 ) {
            v = localDateTimeString.substring( 0, i );
            fraction = localDateTimeString.substring( i + 1 );
        } else {
            v = localDateTimeString;
            fraction = null;
        }
        final DateTimeUtils.PrecisionTime pt = DateTimeUtils.parsePrecisionDateTimeLiteral( v, new SimpleDateFormat( DateTimeUtils.TIMESTAMP_FORMAT_STRING, Locale.ROOT ), this.timeZone, -1 );
        pt.getCalendar().setTimeZone( timeZone );
        if ( fraction != null ) {
            return new TimestampWithTimeZoneString(
                    pt.getCalendar().get( Calendar.YEAR ),
                    pt.getCalendar().get( Calendar.MONTH ) + 1,
                    pt.getCalendar().get( Calendar.DAY_OF_MONTH ),
                    pt.getCalendar().get( Calendar.HOUR_OF_DAY ),
                    pt.getCalendar().get( Calendar.MINUTE ),
                    pt.getCalendar().get( Calendar.SECOND ),
                    timeZone.getID() ).withFraction( fraction );
        }
        return new TimestampWithTimeZoneString(
                pt.getCalendar().get( Calendar.YEAR ),
                pt.getCalendar().get( Calendar.MONTH ) + 1,
                pt.getCalendar().get( Calendar.DAY_OF_MONTH ),
                pt.getCalendar().get( Calendar.HOUR_OF_DAY ),
                pt.getCalendar().get( Calendar.MINUTE ),
                pt.getCalendar().get( Calendar.SECOND ),
                timeZone.getID() );
    }


    @Override
    public String toString() {
        return v;
    }


    @Override
    public boolean equals( Object o ) {
        // The value is in canonical form (no trailing zeros).
        return o == this
                || o instanceof TimestampWithTimeZoneString
                && ((TimestampWithTimeZoneString) o).v.equals( v );
    }


    @Override
    public int hashCode() {
        return v.hashCode();
    }


    @Override
    public int compareTo( TimestampWithTimeZoneString o ) {
        return v.compareTo( o.v );
    }


    public TimestampWithTimeZoneString round( int precision ) {
        Preconditions.checkArgument( precision >= 0 );
        return new TimestampWithTimeZoneString( localDateTime.round( precision ), timeZone );
    }


    /**
     * Creates a TimestampWithTimeZoneString that is a given number of milliseconds since the epoch UTC.
     */
    public static TimestampWithTimeZoneString fromMillisSinceEpoch( long millis ) {
        return new TimestampWithTimeZoneString(
                DateTimeUtils.unixTimestampToString( millis ) + " " + DateTimeUtils.UTC_ZONE.getID() )
                .withMillis( (int) DateTimeUtils.floorMod( millis, 1000 ) );
    }


    /**
     * Converts this TimestampWithTimeZoneString to a string, truncated or padded with zeroes to a given precision.
     */
    public String toString( int precision ) {
        Preconditions.checkArgument( precision >= 0 );
        return localDateTime.toString( precision ) + " " + timeZone.getID();
    }


    public DateString getLocalDateString() {
        return new DateString( localDateTime.toString().substring( 0, 10 ) );
    }


    public TimeString getLocalTimeString() {
        return new TimeString( localDateTime.toString().substring( 11 ) );
    }


    public TimestampString getLocalTimestampString() {
        return localDateTime;
    }

}
