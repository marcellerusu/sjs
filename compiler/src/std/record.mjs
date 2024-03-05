import { ObjectLiteral } from "./globals.mjs";

const Record = Object.freeze({
  keys: Symbol("std/record/Record:keys"),
  values: Symbol("std/record/Record:values"),
});

ObjectLiteral.prototype[Record.keys] = function () {
  return Object.keys(this);
};

ObjectLiteral.prototype[Record.values] = function () {
  return Object.values(this);
};

Map.prototype[Record.keys] = function () {
  return this.keys();
};

Map.prototype[Record.values] = function () {
  return this.values();
};

export default Record;
export const { keys, values } = Record;
