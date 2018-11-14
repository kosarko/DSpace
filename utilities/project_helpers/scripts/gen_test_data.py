#!/usr/bin/python
# -*- coding: utf-8 -*-
import xml.etree.ElementTree as ET
from random import randint


class FixedValueGenerator(object):
    def __init__(self, values):
        self.values = values

    def generate(self):
        return self.values[randint(0, len(self.values) - 1)]


class PlaceGenerator(FixedValueGenerator):
    def __init__(self):
        super(PlaceGenerator, self).__init__([u'Praha', u'Brno', u'Ostrava', u'Kladno', u'Plzeň', u'Horní dolní'])


class DateGenerator:
    def generate(self):
        year = randint(1900, 2018)
        month = randint(1,12)
        day = randint(1, 28)
        return u'{year:04d}-{month:02d}-{day:02d}'.format(year=year, month=month, day=day)


class LengthGenerator:
    def generate(self):
        hours = randint(0,99)
        minutes = randint(0,59)
        seconds = randint(0,59)
        return u'{hours:02d}:{minutes:02d}:{seconds:02d}'.format(hours=hours, minutes=minutes, seconds=seconds)

class IDGenerator:
    def generate(self):
        return u'xyz-{id:08d}'.format(id=randint(1,10000000))


class IsoLangGenerator(FixedValueGenerator):
    def __init__(self):
        super(IsoLangGenerator, self).__init__([u'ces', u'wtf', u'eng', u'svk'])


class NameGenerator:
    maleNameGenerator = FixedValueGenerator([u'Jiří',
                                             u'Jan',
                                             u'Petr',
                                             u'Josef',
                                             u'Pavel',
                                             u'Jaroslav',
                                             u'Martin',
                                             u'Tomáš',
                                             u'Miroslav',
                                             u'František',
                                             u'Zdeněk',
                                             u'Václav',
                                             u'Michal',
                                             u'Karel',
                                             u'Milan',
                                             u'Vladimír',
                                             u'Lukáš',
                                             u'David',
                                             u'Jakub',
                                             u'Ladislav',
                                             u'Stanislav',
                                             u'Roman',
                                             u'Ondřej',
                                             u'Antonín',
                                             u'Radek',
                                             u'Marek',
                                             u'Daniel',
                                             u'Vojtěch',
                                             u'Miloslav',
                                             u'Filip',
                                             ])
    maleSurnameGenerator = FixedValueGenerator([u'Novák',
                                                u'Svoboda',
                                                u'Novotný',
                                                u'Dvořák',
                                                u'Černý',
                                                u'Procházka',
                                                u'Kučera',
                                                u'Veselý',
                                                u'Horák',
                                                u'Němec',
                                                u'Pokorný',
                                                u'Marek',
                                                u'Pospíšil',
                                                u'Hájek',
                                                u'Jelínek',
                                                u'Král',
                                                u'Růžička',
                                                u'Beneš',
                                                u'Fiala',
                                                u'Sedláček',
                                                u'Doležal',
                                                u'Zeman',
                                                u'Nguyen',
                                                ])
    femaleNameGenerator = FixedValueGenerator([
        u'Marie',
        u'Jana',
        u'Eva',
        u'Hana',
        u'Anna',
        u'Lenka',
        u'Kateřina',
        u'Věra',
        u'Lucie',
        u'Alena',
        u'Petra',
        u'Jaroslava',
        u'Veronika',
        u'Martina',
        u'Jitka',
        u'Tereza',
        u'Ludmila',
        u'Helena',
        u'Michaela',
        u'Zdeňka',
        u'Ivana',
        u'Jarmila',
        u'Monika',
        u'Zuzana',
        u'Jiřina',
        u'Markéta',
        u'Eliška',
        u'Marcela',
        u'Barbora',
        u'Dagmar',
    ])
    femaleSurnameGenerator = FixedValueGenerator([
        u'Nováková',
        u'Svobodová',
        u'Novotná',
        u'Dvořáková',
        u'Černá',
        u'Procházková',
        u'Kučerová',
        u'Veselá',
        u'Horáková',
        u'Němcová',
        u'Pokorná',
        u'Marková',
        u'Pospíšilová',
        u'Hájková',
        u'Jelínková',
        u'Králová',
        u'Růžičková',
        u'Benešová',
        u'Fialová',
        u'Sedláčková',
        u'Doležalová',
        u'Zemanová',
        u'Nguyen',
    ])

    def __init__(self, gender=u'nespescifikováno'):
        self.gender = gender

    def generate(self):
        if self.gender == u'žena':
            return NameGenerator.femaleSurnameGenerator.generate() + u', ' + NameGenerator.femaleNameGenerator.generate()
        else:
            return NameGenerator.maleSurnameGenerator.generate() + u', ' + NameGenerator.maleNameGenerator.generate()


class KeyWordGenerator(FixedValueGenerator):
    def __init__(self, prefix):
        self.prefix = prefix
        super(KeyWordGenerator, self).__init__([u'klíč', u'slovo', u'klíčové slovo'])

    def generate(self):
        return self.prefix + super(KeyWordGenerator, self).generate() + str(randint(1,10))




class DefaultValueGenerator:
    def __init__(self, element=u'none'):
        self.element = element

    def generate(self):
        return self.element + str(randint(1,10000))




class Element:
    def __init__(self, schema, element, qualifier=u'none', repeatable=False, language=u'en_US',
                 generator=None):
        self.schema = schema
        self.element = element
        self.qualifier = qualifier
        self.repeatable = repeatable
        self.language = language
        self.generator = generator
        self.generator = DefaultValueGenerator(element) if generator is None else generator



NARRATOR_ELEMENTS = {u'viadat':[
    Element(u'viadat', u'narrator', u'gender', generator=FixedValueGenerator([u'muž', u'žena', u'nespecifikováno'])),
    Element(u'viadat', u'narrator', u'birthdate', generator=DateGenerator()),
    Element(u'viadat', u'narrator', u'alias', repeatable=True, generator=NameGenerator()),
    Element(u'viadat', u'narrator', u'degree', repeatable=True, generator=FixedValueGenerator([u'Ing.', u'Mgr.', u'Bc.', u'Mudr.', u'Ph.d.'])),
    Element(u'viadat', u'narrator', u'keywordsProfession', repeatable=True, generator=KeyWordGenerator(prefix=u'prof_')),
    Element(u'viadat', u'narrator', u'keywordsTopic', repeatable=True, generator=KeyWordGenerator(prefix=u'tema_')),
    Element(u'viadat', u'narrator', u'contact'),
    Element(u'viadat', u'narrator', u'consent', generator=FixedValueGenerator([u'Ano', u'Ne', u'Včetně dalších '
                                                                                         u'ujednání'])),
    Element(u'viadat', u'narrator', u'note'),
    Element(u'viadat', u'narrator', u'project')

], u'dc': [
    Element(u'dc', u'title', generator=NameGenerator()),
    Element(u'dc', u'identifier', generator=IDGenerator()),
    Element(u'dc', u'type', generator=FixedValueGenerator([u'narrator'])),
    Element(u'dc', u'rights', u'uri', generator=FixedValueGenerator([ u'https://ufal.mff.cuni.cz/grants/viadat/license'])),
    Element(u'dc', u'rights', generator=FixedValueGenerator([u'VIADAT License'])),
    Element(u'dc', u'rights', u'label', generator=FixedValueGenerator([u'RES']))
]}

INTERVIEW_ELEMENTS = {u'viadat': [
    Element(u'viadat', u'interview', u'transcript', generator=FixedValueGenerator([u'Doslovný', u'Redigovaný',
                                                                                u'Orientační', u'Ne'])),
    Element(u'viadat', u'interview', u'date', generator=DateGenerator()),
    Element(u'viadat', u'interview', u'length', generator=LengthGenerator()),
    Element(u'viadat', u'interview', u'place', generator=PlaceGenerator()),
    Element(u'viadat', u'interview', u'interviewer', generator=NameGenerator()),
    Element(u'viadat', u'interview', u'keywords', repeatable=True, generator=KeyWordGenerator(prefix=u'int_')),
    Element(u'viadat', u'interview', u'detailedKeywords', repeatable=True, generator=KeyWordGenerator(prefix=u'det_')),
    Element(u'viadat', u'interview', u'period'),
    Element(u'viadat', u'interview', u'type', generator=FixedValueGenerator([u'životopisný', u'životopisně-tematický',
                                                         u'tematický', u'ostatní'])),
    Element(u'viadat', u'interview', u'note')
], u'dc': [
    Element(u'dc', u'title'),
    Element(u'dc', u'identifier', generator=IDGenerator()),
    Element(u'dc', u'type', generator=FixedValueGenerator([u'interview'])),
    Element(u'dc', u'description'),
    Element(u'dc', u'language', u'iso', generator=IsoLangGenerator()),
    Element(u'dc', u'rights', u'uri', generator=FixedValueGenerator([u'https://ufal.mff.cuni.cz/grants/viadat/license'])),
    Element(u'dc', u'rights', generator=FixedValueGenerator([u'VIADAT License'])),
    Element(u'dc', u'rights', u'label', generator=FixedValueGenerator([u'RES']))

]}

# TODO all the fields!
    # Element('dc', 'creator')
    # Element('viadat', 'output'),
    # Element('viadat', 'technical', 'note'),
    # Element('viadat', 'technical', 'type', generator=FixedValueGenerator(['audio', 'audio-vizuální', 'text'])),


file_to_elements = {u'dublin_core.xml': u'dc', u'metadata_viadat.xml': u'viadat' }


def gen_item(fields):
    gender = u'nespecifikováno'
    for metadata_file in [u'dublin_core.xml', u'metadata_viadat.xml']: #, u'metadata_local.xml']:
        elements = fields[file_to_elements[metadata_file]]
        schema = elements[0].schema
        dublin_core = ET.Element(u'dublin_core', attrib={u'schema': schema})
        for element in elements:
            repeat = 1 if not element.repeatable else randint(1,5)
            # XXX
            element.generator.gender = gender
            for i in range(0,repeat):
                dcvalue = ET.SubElement(dublin_core, u'dcvalue', attrib={u'element': element.element, u'qualifier':
                    element.qualifier})
                dcvalue.text = element.generator.generate()
                if dcvalue.text in [u'muž', u'žena', u'nespecifikováno']:
                    gender = dcvalue.text

        tree = ET.ElementTree(dublin_core)
        tree.write(metadata_file, encoding='utf-8', xml_declaration=True)


if __name__ == '__main__':
    import sys
    if sys.argv[1] == u'narrator':
        gen_item(NARRATOR_ELEMENTS)
    elif sys.argv[1] == u'interview':
        gen_item(INTERVIEW_ELEMENTS)
    else:
        sys.exit(u'Unknown type {}'.format(sys.argv[1]))


# for 1 to examples_count do
# gen contents
# gen dublin_core.xml
# gen handle
# gen license.txt
# gen metadata_local.xml
# gen metadata_viadat.xml

# for metadata_files
# gen metadata_elements

# for metadata_elements
# gen value
# repeat?
