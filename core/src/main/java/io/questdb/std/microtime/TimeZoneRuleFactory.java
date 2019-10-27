/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2019 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package io.questdb.std.microtime;

import io.questdb.std.CharSequenceIntHashMap;
import io.questdb.std.Numbers;
import io.questdb.std.ObjList;

import java.time.ZoneId;
import java.util.Map;

public class TimeZoneRuleFactory {

    public static final TimeZoneRuleFactory INSTANCE = new TimeZoneRuleFactory();

    private final CharSequenceIntHashMap ruleMap = new CharSequenceIntHashMap();
    private final ObjList<TimeZoneRules> ruleList = new ObjList<>();

    public TimeZoneRuleFactory() {
        int index = 0;
        for (String z : ZoneId.getAvailableZoneIds()) {
            ruleList.add(new TimeZoneRulesImpl(ZoneId.of(z).getRules()));
            ruleMap.put(z, index++);
        }

        for (Map.Entry<String, String> e : ZoneId.SHORT_IDS.entrySet()) {
            String key = e.getKey();
            String alias = e.getValue();

            // key already added somehow?
            int i = ruleMap.get(key);
            if (i == -1) {
                // no, good, add
                i = ruleMap.get(alias);
                if (i == -1) {
                    // this could be fixed offset, try parsing value as one
                    long offset = Timestamps.parseOffset(alias, 0, alias.length());
                    if (offset != Long.MIN_VALUE) {
                        ruleList.add(new FixedTimeZoneRule(Numbers.decodeLowInt(offset) * Timestamps.MILLI_MICROS));
                        ruleMap.put(key, index++);
                    }
                } else {
                    ruleMap.put(key, i);
                }
            }
        }
    }

    public int getTimeZoneRulesIndex(CharSequence id) {
        return ruleMap.get(id);
    }

    public TimeZoneRules getTimeZoneRulesQuick(int index) {
        return ruleList.getQuick(index);
    }
}
