import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import CheckboxRadioSNCF from 'common/BootstrapSNCF/CheckboxRadioSNCF';

export default function SpeedSpaceSettings() {
  const dispatch = useDispatch();
  const { speedSpaceSettings } = useSelector((state) => state.osrdsimulation);
  return (
    <div />
  );
}