﻿using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Animation;
using IsolatedStorageExplorerClient.Client;
using IsolatedStorageExplorerClient.ClientOperationService;

namespace IsolatedStorageExplorerClient.UI.Controls.OperationsPanel
{
    /// <summary>
    /// Interaction logic for DeleteFileOperation.xaml
    /// </summary>
    public partial class DeleteFileOperation : UserControl
    {
        public DeleteFileOperation()
        {
            InitializeComponent();
        }

        private static Operation GetNewOperation(Guid appSessionId)
        {
            return new Operation()
            {
                Id = Guid.NewGuid(),
                ClientSessionId = ExplorerClient.Instance.SessionToken,
                ApplicationSessionId = appSessionId,
                OperationsToExecute = new List<RequestBase>()
            };
        }


        public void Show(double panelWidth)
        {
            Measure(new Size(panelWidth, double.MaxValue));
            var size = DesiredSize;
            var animation = new DoubleAnimation
            {
                To = size.Height,
                From = 0,
                Duration = TimeSpan.FromMilliseconds(200)
            };
            BeginAnimation(HeightProperty, animation);
        }


        public void Hide()
        {
            var animation = new DoubleAnimation { To = 0, Duration = TimeSpan.FromMilliseconds(200) };
            BeginAnimation(HeightProperty, animation);
        }

        public void DeleteFile(FileInformation mappedFile, ApplicationInstance currentApplication, double actualWidth, TreeView storageTreeView)
        {
            var op = GetNewOperation(currentApplication.ApplicationSessionId);
            var createDirRequest = new DeleteFileRequest()
            {
                Id = Guid.NewGuid(),
                FilePath = mappedFile.Path
            };
            op.OperationsToExecute.Add(createDirRequest);
            ExplorerClient.Instance.OperationProgressReceived +=
                (id, s) =>
                {
                    if (s.OperationId != op.Id) return;
                    storageTreeView.IsEnabled = true;
                    Hide();
                    if (s is OperationFailedDueToException)
                    {
                        MessageBox.Show(
                            "Deleting the file '" + mappedFile.FileName + "' has failed.\nPlease check your changes and try again.",
                            "WP7 Isolated Storage Explorer", MessageBoxButton.OK, MessageBoxImage.Error);
                    }
                };
            Show(actualWidth);
            ExplorerClient.Instance.PerformOperation(op);
            storageTreeView.IsEnabled = false;
        }
    }
}