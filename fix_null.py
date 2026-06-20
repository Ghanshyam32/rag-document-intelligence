path = r'C:\Users\ghanshyam\Downloads\empty-stack\src\main\java\com\ghanshyam\empty_stack\service\DocumentService.java'
content = open(path, 'rb').read().decode('utf-8')
# Replace the broken empty-string replace with proper Java unicode escape for null byte
old = '.replace("", "")'
new = '.replace("\\u0000", "")'
if old in content:
    content = content.replace(old, new)
    print("Replaced successfully")
else:
    print("Pattern not found, current replace line:")
    for i, line in enumerate(content.splitlines()):
        if 'replace' in line and 'chunkText' in line:
            print(i+1, repr(line))
open(path, 'wb').write(content.encode('utf-8'))
print("Done")
