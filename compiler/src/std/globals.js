"use strict";

export function ObjectLiteral(entries) {
  Object.assign(this, Object.fromEntries(entries));
}

ObjectLiteral.prototype[Symbol.iterator] = function* () {
  for (let key in this) {
    yield [key, this[key]];
  }
};

export function Nil() {}

export let nil = new Proxy(new Nil(), {
  get(target, property, _receiver) {
    if (property in target) {
      return target[property];
    } else if (property === Symbol.toStringTag) {
      return "Nil";
    } else {
      throw new TypeError(`Nil does not have property: ${property.toString()}`);
    }
  },
});

export class Keyword {
  static cache = new Map();
  static for(name) {
    if (this.cache.get(name)) {
      return Keyword.cache.get(name);
    } else {
      let kw = new Keyword(name);
      Keyword.cache.set(name, kw);
      return kw;
    }
  }
  constructor(value) {
    this.value = value;
  }
  [Symbol.toPrimitive]() {
    return this.value;
  }
}

export function dot(lhs, rhs) {
  let result = lhs[rhs];
  if (typeof result === "function") {
    return result.bind(lhs);
  } else {
    return result ?? nil;
  }
}
