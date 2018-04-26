export function endsWithCaseInsensitive(fullString: string, substring: string): boolean {
  fullString = fullString.toLowerCase();
  substring = substring.toLowerCase();
  let idx = fullString.lastIndexOf(substring);
  if (idx === -1) {
    return false;
  }
  return (fullString.length - idx) === substring.length;
}
