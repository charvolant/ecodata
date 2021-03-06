package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

class DateConstantGetter implements Getter<String> {
    def parser

    def name, value

    public DateConstantGetter(name, value, lat = null, lng = null, style = DateTimeParser.Style.DATETIME) {
        this.parser = new DateTimeParser(style)
        this.name = name
        if (value instanceof Date) {
            this.value = parser.parse(value, lat, lng) ?: value
        } else if (value instanceof String) {
            this.value = parser.parse(value, lat, lng) ?: value
        } else {
            this.value = value
        }
    }

    @Override
    String getPropertyName() {
        return name
    }

    @Override
    String getFormattedValue(object) {
        parser.format(value)
    }

    String toString() {
        return "${name} = ${value}"
    }
}