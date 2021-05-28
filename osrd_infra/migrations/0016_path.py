# Generated by Django 3.2.2 on 2021-05-28 06:18

import django.contrib.gis.db.models.fields
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('osrd_infra', '0015_alter_track_location_component'),
    ]

    operations = [
        migrations.CreateModel(
            name='Path',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(max_length=128)),
                ('owner', models.UUIDField(default='00000000-0000-0000-0000-000000000000', editable=False)),
                ('created', models.DateTimeField(auto_now_add=True)),
                ('payload', models.JSONField()),
                ('geographic', django.contrib.gis.db.models.fields.LineStringField(editable=False, srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.LineStringField(editable=False, srid=3857)),
                ('namespace', models.ForeignKey(editable=False, on_delete=django.db.models.deletion.CASCADE, to='osrd_infra.entitynamespace')),
            ],
        ),
    ]
