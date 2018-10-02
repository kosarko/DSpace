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
        super(PlaceGenerator, self).__init__(['Praha', 'Brno', 'Ostrava', 'Kladno', 'Plzeň', 'Horní dolní'])


class DateGenerator:
    def generate(self):
        year = randint(1900, 2018)
        month = randint(1,12)
        day = randint(1, 28)
        return '{year:04d}-{month:02d}-{day:02d}'.format(year=year, month=month, day=day)


class LengthGenerator:
    def generate(self):
        hours = randint(0,99)
        minutes = randint(0,59)
        seconds = randint(0,59)
        return '{hours:02d}:{minutes:02d}:{seconds:02d}'.format(hours=hours, minutes=minutes, seconds=seconds)

class IDGenerator:
    def generate(self):
        return 'xyz-{id:08d}'.format(id=randint(1,10000000))


class IsoLangGenerator(FixedValueGenerator):
    def __init__(self):
        super(IsoLangGenerator, self).__init__(['ces', 'wtf', 'eng', 'svk'])


class NameGenerator:
    maleNameGenerator = FixedValueGenerator(['Jiří',
                                             'Jan',
                                             'Petr',
                                             'Josef',
                                             'Pavel',
                                             'Jaroslav',
                                             'Martin',
                                             'Tomáš',
                                             'Miroslav',
                                             'František',
                                             'Zdeněk',
                                             'Václav',
                                             'Michal',
                                             'Karel',
                                             'Milan',
                                             'Vladimír',
                                             'Lukáš',
                                             'David',
                                             'Jakub',
                                             'Ladislav',
                                             'Stanislav',
                                             'Roman',
                                             'Ondřej',
                                             'Antonín',
                                             'Radek',
                                             'Marek',
                                             'Daniel',
                                             'Vojtěch',
                                             'Miloslav',
                                             'Filip',
                                             ])
    maleSurnameGenerator = FixedValueGenerator(['Novák',
                                                'Svoboda',
                                                'Novotný',
                                                'Dvořák',
                                                'Černý',
                                                'Procházka',
                                                'Kučera',
                                                'Veselý',
                                                'Horák',
                                                'Němec',
                                                'Pokorný',
                                                'Marek',
                                                'Pospíšil',
                                                'Hájek',
                                                'Jelínek',
                                                'Král',
                                                'Růžička',
                                                'Beneš',
                                                'Fiala',
                                                'Sedláček',
                                                'Doležal',
                                                'Zeman',
                                                'Nguyen',
                                                ])
    femaleNameGenerator = FixedValueGenerator([
        'Marie',
        'Jana',
        'Eva',
        'Hana',
        'Anna',
        'Lenka',
        'Kateřina',
        'Věra',
        'Lucie',
        'Alena',
        'Petra',
        'Jaroslava',
        'Veronika',
        'Martina',
        'Jitka',
        'Tereza',
        'Ludmila',
        'Helena',
        'Michaela',
        'Zdeňka',
        'Ivana',
        'Jarmila',
        'Monika',
        'Zuzana',
        'Jiřina',
        'Markéta',
        'Eliška',
        'Marcela',
        'Barbora',
        'Dagmar',
    ])
    femaleSurnameGenerator = FixedValueGenerator([
        'Nováková',
        'Svobodová',
        'Novotná',
        'Dvořáková',
        'Černá',
        'Procházková',
        'Kučerová',
        'Veselá',
        'Horáková',
        'Němcová',
        'Pokorná',
        'Marková',
        'Pospíšilová',
        'Hájková',
        'Jelínková',
        'Králová',
        'Růžičková',
        'Benešová',
        'Fialová',
        'Sedláčková',
        'Doležalová',
        'Zemanová',
        'Nguyen',
    ])

    def __init__(self, gender='nespescifikováno'):
        self.gender = gender


    def generate(self):
        if self.gender == u'žena':
            return NameGenerator.femaleSurnameGenerator.generate() + ', ' + NameGenerator.femaleNameGenerator.generate()
        else:
            return NameGenerator.maleSurnameGenerator.generate() + ', ' + NameGenerator.maleNameGenerator.generate()


class KeyWordGenerator(FixedValueGenerator):
    def __init__(self, prefix):
        self.prefix = prefix
        super(KeyWordGenerator, self).__init__(['klíč', 'slovo', 'klíčové slovo'])

    def generate(self):
        return self.prefix + super(KeyWordGenerator, self).generate() + str(randint(1,10))




class DefaultValueGenerator:
    def __init__(self, element='none'):
        self.element = element

    def generate(self):
        return self.element + str(randint(1,10000))




class Element:
    def __init__(self, schema, element, qualifier='none', repeatable=False, language='en_US',
                 generator=None):
        self.schema = schema
        self.element = element
        self.qualifier = qualifier
        self.repeatable = repeatable
        self.language = language
        self.generator = generator
        self.generator = DefaultValueGenerator(element) if generator is None else generator



NARRATOR_ELEMENTS = {'viadat':[
    Element('viadat', 'narrator', 'gender', generator=FixedValueGenerator(['muž', 'žena', 'nespecifikováno'])),
    Element('viadat', 'narrator', 'birthdate', generator=DateGenerator()),
    Element('viadat', 'narrator', 'identifier', generator=IDGenerator()),
    Element('viadat', 'narrator', 'alias', repeatable=True, generator=NameGenerator()),
    Element('viadat', 'narrator', 'degree', repeatable=True, generator=FixedValueGenerator(['Ing.', 'Mgr.', 'Bc.', 'Mudr.', 'Ph.d.'])),
    Element('viadat', 'narrator', 'keywordsProfession', repeatable=True, generator=KeyWordGenerator(prefix='prof_')),
    Element('viadat', 'narrator', 'keywordsTopic', repeatable=True, generator=KeyWordGenerator(prefix='tema_')),
    Element('viadat', 'narrator', 'contact'),
    Element('viadat', 'narrator', 'consent', generator=FixedValueGenerator(['Ano', 'Ne', 'Včetně dalších '
                                                                                         'ujednání'])),
    Element('viadat', 'narrator', 'note'),
    Element('viadat', 'narrator', 'project')

], 'dc': [
    Element('dc', 'title', generator=NameGenerator()),
    Element('dc', 'type', generator=FixedValueGenerator(['narrator']))
]}

INTERVIEW_ELEMENTS = {'viadat': [
    Element('viadat', 'interview', 'identifier', generator=IDGenerator()),
    Element('viadat', 'interview', 'transcript', generator=FixedValueGenerator(['Doslovný', 'Redigovaný',
                                                                                'Orientační', 'Ne'])),
    Element('viadat', 'interview', 'date', generator=DateGenerator()),
    Element('viadat', 'interview', 'length', generator=LengthGenerator()),
    Element('viadat', 'interview', 'place', generator=PlaceGenerator()),
    Element('viadat', 'interview', 'interviewer', generator=NameGenerator()),
    Element('viadat', 'interview', 'keywords', repeatable=True, generator=KeyWordGenerator(prefix='int_')),
    Element('viadat', 'interview', 'detailedKeywords', repeatable=True, generator=KeyWordGenerator(prefix='det_')),
    Element('viadat', 'interview', 'period'),
    Element('viadat', 'interview', 'type', generator=FixedValueGenerator(['životopisný', 'životopisně-tematický',
                                                         'tematický', 'ostatní'])),
    Element('viadat', 'interview', 'note')
], 'dc': [
    Element('dc', 'title'),
    Element('dc', 'type', generator=FixedValueGenerator(['interview'])),
    Element('dc', 'description'),
    Element('dc', 'language', 'iso', generator=IsoLangGenerator())

]}

# TODO all the fields!
    # Element('dc', 'creator')
    # Element('viadat', 'output'),
    # Element('viadat', 'technical', 'note'),
    # Element('viadat', 'technical', 'type', generator=FixedValueGenerator(['audio', 'audio-vizuální', 'text'])),


file_to_elements = {'dublin_core.xml': 'dc', 'metadata_viadat.xml': 'viadat' }


def gen_item(fields):
    gender = 'nespecifikováno'
    for metadata_file in ['dublin_core.xml', 'metadata_viadat.xml']: #, 'metadata_local.xml']:
        elements = fields[file_to_elements[metadata_file]]
        schema = elements[0].schema
        dublin_core = ET.Element('dublin_core', attrib={'schema': schema})
        for element in elements:
            repeat = 1 if not element.repeatable else randint(1,5)
            # XXX
            element.generator.gender = gender
            for i in range(0,repeat):
                dcvalue = ET.SubElement(dublin_core, 'dcvalue', attrib={'element': element.element, 'qualifier':
                    element.qualifier})
                dcvalue.text = element.generator.generate().decode('utf-8')
                if dcvalue.text in [u'muž', u'žena', u'nespecifikováno']:
                    gender = dcvalue.text

        tree = ET.ElementTree(dublin_core)
        tree.write(metadata_file, encoding='utf-8')


if __name__ == '__main__':
    import sys
    if sys.argv[1] == 'narrator':
        gen_item(NARRATOR_ELEMENTS)
    elif sys.argv[1] == 'interview':
        gen_item(INTERVIEW_ELEMENTS)
    else:
        sys.exit('Unknown type {}'.format(sys.argv[1]))


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
