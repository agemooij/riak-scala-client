package com.scalapenos.riak
package resolvers


case object LastValueWinsResolver extends ConflictResolver {
  def resolve(values: Set[RiakValue]): RiakValue = {
    values.reduceLeft { (first, second) =>
      if (second.lastModified.isAfter(first.lastModified)) second
      else first
    }
  }
}
