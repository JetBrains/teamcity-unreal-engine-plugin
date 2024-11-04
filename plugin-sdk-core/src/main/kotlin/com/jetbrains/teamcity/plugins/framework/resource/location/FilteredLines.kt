package com.jetbrains.teamcity.plugins.framework.resource.location

import java.io.Reader

/**
 * Predicate used to select only matching lines.
 */
typealias AcceptFilter = (line: String) -> Boolean

/**
 * Predicate used to determine whether reading should be continued.
 */
typealias ReadContinuationDecision = (line: String, acceptedSoFar: Int) -> Boolean

@PublishedApi
internal fun Reader.filteredLines(
    accept: AcceptFilter,
    continueReading: ReadContinuationDecision
): List<String> = buildList {
    useLines {
        for (line in it) {
            if (accept(line)) {
                add(line)
            }

            if (continueReading(line, size)) {
                continue
            }

            break
        }
    }
}
