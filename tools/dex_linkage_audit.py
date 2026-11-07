import struct, sys
exec(open('/tmp/dexcheck.py').read().split('FRAMEWORK=')[0])
FW=('Landroid/','Ljava/','Ljavax/','Ldalvik/','Lorg/json/','Lorg/xmlpull/','Lorg/w3c/','Lorg/xml/sax','Lsun/misc/')
missing=set()
# superclass + interfaces of defined classes
for i in range(class_defs_size):
    base=class_defs_off+i*32
    class_idx=u32(base); super_idx=u32(base+8); iface_off=u32(base+12)
    for t in [types[super_idx] if super_idx!=0xffffffff else None]:
        if t and not t.startswith('[') and not t.startswith(FW) and t not in defined: missing.add(('super',t))
    if iface_off:
        sz=u32(iface_off)
        for j in range(sz):
            t=types[struct.unpack_from("<H", data, iface_off+4+j*2)[0]]
            if not t.startswith(FW) and t not in defined: missing.add(('iface',t))
# field refs
for i in range(field_ids_size):
    cls=struct.unpack_from('<H', data, field_ids_off+i*8)[0]; cname=types[cls]
    if cname.startswith('[') or cname.startswith(FW): continue
    if cname not in defined: missing.add(('fieldclass',cname))
# method refs (as before)
for i in range(method_ids_size):
    cname,_=method_key(i)
    if cname.startswith('[') or cname.startswith(FW): continue
    if cname not in defined: missing.add(('methodclass',cname))
for k,c in sorted(missing): print(k,c)
print('total:',len(missing))
